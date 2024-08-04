package me.kaigermany.ultimateutils.io.externalstorage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.WeakHashMap;

public class DynamicByteArrayStorage {
	public static void demo(){
		final DynamicByteArrayStorage storage = new DynamicByteArrayStorage();
		  try{
		    storage.registerNewStorage(new File("C:/FirstStorage.File"), 1L << 30);//1GB
		    storage.registerNewStorage(new File("D:/foo/NextStorageFile.dat"), 20L << 30);//20GB
		  }catch(Exception e){e.printStackTrace();}
		  storage.setMaxRuntimeMemory(2L << 30);//tell this manager: if it contains more then 2GB then expand on disk to free up runtime space.
		  
		  //alloc 1 MB
		  byte[] data = new byte[1 << 20];
		  DynamicByteArray dataPtr = storage.allocateSpace(data);
		  data = null;//drop local pointer.
		  
		  //alloc another 2GB of ram:
		  DynamicByteArray dataPtr2 = storage.allocateSpace(2 << 30);
		  
		  //dataPtr should be expanded into 2nd storage file.
		  
		  dataPtr = null;
		  System.gc();
		  //drop dataPtr's bytes from file 2.
		  
		  //get save access to data2's bytes.
		  data = dataPtr2.getData();
		  
		  //do your stuff...
		  data[123] = 45;
		  
		  //set this pointer back!
		  //if not done and the file randomly getting expanded to disk,
		  //you will have data loss!!!
		  //you have to notify the system that you have changed something,
		  //so the system can react to it and replace the old data on disk.
		  dataPtr2.setData(data);
		  
		  
	}
	
	//containing only runtime-loaded entries.
	private volatile WeakHashMap<DynamicByteArray, Object> buffers = new WeakHashMap<DynamicByteArray, Object>(4096);
	private volatile long localAllocatedBytes;
	private volatile long maxLocalAllocatedBytes;

	private ArrayList<DiskStorageFile> storages = new ArrayList<DiskStorageFile>();

	public DynamicByteArrayStorage() {
	}

	public void setMaxRuntimeMemory(long max) {
		maxLocalAllocatedBytes = max;
		checkForUnloading();
	}

	public DynamicByteArrayStorage registerNewStorage(File file, long maxFileSize) throws IOException {
		storages.add(new DiskStorageFile(file, maxFileSize));
		return this;
	}

	public DynamicByteArray allocateSpace(int len) {
		return allocateSpace(new byte[len]);
	}

	public DynamicByteArray allocateSpace(byte[] initialDataPtr) {
		DynamicByteArray dba;
		synchronized (buffers) {
			checkForUnloading();
			dba = new DynamicByteArray();
			dba.localBuffer = initialDataPtr;
			dba.manager = this;
			dba.lastAccessTime = System.currentTimeMillis();
			buffers.put(dba, null);
			localAllocatedBytes += initialDataPtr.length;
		}
		return dba;
	}

	public void free(DynamicByteArray dba) {
		if (dba.manager != this) {
			dba.manager.free(dba);
			return;
		}
		synchronized (buffers) {
			buffers.remove(dba);
			DiskStorageFile ext = dba.externalBuffer;
			if (ext != null) {
				try {
					ext.remove(dba);
				} catch (IOException e) {
					// we don't really expect crashes here, because we only deallocate stuff.
					e.printStackTrace();
				}
			} else {
				int len = dba.localBuffer.length;
				localAllocatedBytes -= len;
			}
		}
	}

	final long unloadingCooldown = 1000;
	long unloadingTimeout = 0;
	
	//checks if there must be freed up some ram.
	// must be called inside synchronized(buffers){} statement!
	private void checkForUnloading() {
		long t = System.currentTimeMillis();
		if (t < unloadingTimeout) {
			return;
		}
		unloadingTimeout = t + unloadingCooldown;

		long bytesUnloaded = 0;
		while (localAllocatedBytes > maxLocalAllocatedBytes && buffers.size() > 1) {
			long lastAccessTime = Long.MAX_VALUE;
			DynamicByteArray arr = null;
			for(DynamicByteArray next : buffers.keySet()) {
				if (next == null) continue;
				if (next.lastAccessTime < lastAccessTime) {
					lastAccessTime = next.lastAccessTime;
					arr = next;
				}
			}
			if (arr != null) {
				int len = arr.localBuffer.length;
				if (!tryMoveToDisk(arr))
					break;
				buffers.remove(arr);
				localAllocatedBytes -= len;
				bytesUnloaded += len;
			}
		}
	}
	
	//tries to find the next best space on disk.
	//returns false if operation was not completed, because of too less space left.
	private boolean tryMoveToDisk(DynamicByteArray data) {
		try {
			for (DiskStorageFile disk : storages) {
				if (disk.tryWrite(data)){
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	//read from storage file and remove its entry.
	private void streamBackIntoRam(DynamicByteArray data) {
		if (data.externalBuffer == null){
			return;
		}
		if (data.manager != this) {
			data.manager.streamBackIntoRam(data);
			return;
		}
		synchronized (buffers) {
			try {
				data.externalBuffer.readAndRemove(data);
				buffers.put(data, null);
				localAllocatedBytes += data.localBuffer.length;
				data.lastAccessTime = System.currentTimeMillis();
				checkForUnloading();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class DynamicByteArray {
		private volatile byte[] localBuffer;
		private volatile DiskStorageFile externalBuffer;
		private volatile long externalBufferPos;
		private volatile int externalBufferLen;
		private DynamicByteArrayStorage manager;
		private volatile long lastAccessTime;

		protected DynamicByteArray() {
		}

		public byte[] getData() {
			ensureLoaded();
			return localBuffer;
		}

		public void setData(byte[] data) {
			ensureLoaded();
			localBuffer = data;
		}

		private void ensureLoaded() {
			synchronized (this) {
				if (localBuffer == null) {
					manager.streamBackIntoRam(this);
					externalBuffer = null;
					externalBufferPos = 0;
					externalBufferLen = 0;
				}
				lastAccessTime = System.currentTimeMillis();
			}
		}

		@Override
		public void finalize() throws Throwable {
			if (localBuffer != null)
				manager.free(this);
		}
	}

	public static class DiskStorageFile {
		private final int pageSize;
		private RandomAccessFile raf;
		private long fileLength;
		// private long maxFileLength;
		private volatile BitMapTable bitmap;
		private volatile HashSet<long[]> allocatedRegionRefs = new HashSet<long[]>(1024);
		private final File fileOnDrive;
		
		public DiskStorageFile(File file, long maxFileLength) throws IOException {
			this(file, maxFileLength, 4096);
		}

		public DiskStorageFile(File file, long maxFileLength, int pageSize) throws IOException {
			this.pageSize = pageSize;
			this.fileOnDrive = file;
			this.raf = new RandomAccessFile(file, "rwd");
			long maxFilePagesLength = maxFileLength / pageSize;
			this.bitmap = new BitMapTable(maxFilePagesLength);
			fileLength = 0;
			raf.setLength(fileLength);
		}

		public boolean tryWrite(DynamicByteArray data) throws IOException {
			synchronized (this) {
				long numPages = data.localBuffer.length;
				numPages = (numPages / pageSize) + (numPages % pageSize != 0 ? 1 : 0);
				long pos = bitmap.findFreePageBlock(numPages);
				if (pos == -1)
					return false;

				bitmap.setPages(pos, numPages, true);
				data.externalBufferPos = pos;
				data.externalBufferLen = data.localBuffer.length;
				data.externalBuffer = this;
				write(data);

				checkAdd(pos, numPages);
			}
			// data.localBuffer = null;
			return true;
		}

		private void checkAdd(long pos, long numPages) {
			for (long[] a : allocatedRegionRefs) {
				long existingPos = a[0];
				long existingLen = a[1];
				// Check if the new range [pos, pos + len) overlaps with the
				// existing range [existingPos, existingPos + existingLen)
				if (pos < existingPos + existingLen && pos + numPages > existingPos) {
					System.err.println("collision predicted!!! -> collision while add");
					return; // Overlap found
				}
			}
			allocatedRegionRefs.add(new long[] { pos, numPages });
		}

		private void checkRemove(long pos, long numPages) {
			long[] found = null;
			for (long[] a : allocatedRegionRefs) {
				long existingPos = a[0];
				long existingLen = a[1];
				if (existingPos == pos && numPages == existingLen) {
					found = a;
					break; // found
				}
			}
			if (found == null) {
				System.err.println("error predicted!!! -> missing remove");
			} else {
				allocatedRegionRefs.remove(found);
			}
		}

		public void readAndRemove(DynamicByteArray data) throws IOException {
			synchronized (this) {

				synchronized (raf) {
					long pos = data.externalBufferPos * pageSize;
					raf.seek(pos);
					byte[] out = new byte[data.externalBufferLen];

					//log("read " + data.toString() + " from " + pos + " +" + out.length + " -> " + (pos + out.length));

					raf.readFully(out);
					data.localBuffer = out;
				}
				remove(data);

			}
		}

		public void remove(DynamicByteArray data) throws IOException {
			synchronized (this) {
				long numPages = data.externalBufferLen;
				numPages = (numPages / pageSize) + (numPages % pageSize != 0 ? 1 : 0);
				bitmap.setPages(data.externalBufferPos, numPages, false);
				synchronized (raf) {
					long oldLen = fileLength;
					long lastPageInUse = bitmap.getHighestUsedIndex();
					lastPageInUse++;// seek to the end of this page.
					fileLength = lastPageInUse * pageSize;// scale up to
															// byte-level.
					if (fileLength < oldLen) {
						raf.setLength(fileLength);
						// log("remove: shrink file to " + fileLength);
					}
				}
				checkRemove(data.externalBufferPos, numPages);
			}
		}

		private boolean write(DynamicByteArray data) throws IOException {
			synchronized (raf) {
				long pos = data.externalBufferPos * pageSize;
				{
					long localLen = pos + data.externalBufferLen;
					if (localLen > fileLength) {
						if(!checkDiskCapacity(data.externalBufferLen)) return false;
						fileLength = localLen;
						raf.setLength(fileLength);
					}
				}
				raf.seek(pos);
				byte[] out = data.localBuffer;
				data.localBuffer = null;
				raf.write(out);

				//log("write " + data.toString() + " to " + pos + " +" + out.length + " -> " + (pos + out.length));

				// data.localBuffer = out;
			}
			return true;
		}
		
		private boolean checkDiskCapacity(long additionalBytesNeeded){
			additionalBytesNeeded += 100 << 20;//lets left at lest 100 MB free for FileSystem or Operating System stuff...
			return fileOnDrive.getUsableSpace() > additionalBytesNeeded;
		}
	}

	public static class BitMapTable {
		public volatile long[] map;
		private final long maxIndex;

		public BitMapTable(long maxNumEntries) {
			maxIndex = maxNumEntries;
			map = new long[(int) (maxIndex / 64) + (maxIndex % 64 != 0 ? 1 : 0)];
		}

		public int get(long index) {
			if (index < 0 || index >= maxIndex)
				return -1;
			return (int) ((map[(int) (index / 64)] >> (index % 64)) & 1);
		}

		public long getHighestUsedIndex() {
			for (int i = map.length - 1; i >= 0; i--) {
				long word = map[i];
				if (word != 0) {
					long index = (long) i * 64 + 63;
					for (int ii = 63; ii >= 0; ii--) {
						if (get(index) == 1) {// if used
							index++;
							return index;
						} else {// if unused or out-of-bounds
							index--;
						}
					}
				}
			}
			return -1;// no index is in use!
		}

		public void setPages(long start, long len, boolean setBits) {
			if (start < 0 || start >= maxIndex || len <= 0 || start + len > maxIndex)
				return;
			long end = start + len;
			if (setBits) {
				for (long i = start; i < end; i++) {
					int pos = (int) (i / 64);
					int bit = (int) (i % 64);
					map[pos] |= (1L << bit);
				}
			} else {
				for (long i = start; i < end; i++) {
					int pos = (int) (i / 64);
					int bit = (int) (i % 64);
					map[pos] &= ~(1L << bit);
				}
			}
		}

		public long findFreePageBlock(long numPages) {
			if (numPages <= 0 || numPages > maxIndex) {
				return -1;
			}
			boolean mustFindEmptyBlocks = numPages >= 192; // 64 * 3

			long contiguousCount = 0;
			long startIndex = -1;

			for (int i = 0; i < map.length; i++) {
				long word = map[i];

				if (word == -1L) { // Skip fully occupied blocks
					startIndex = -1;
					contiguousCount = 0;
					continue;
				}

				if (mustFindEmptyBlocks && word == 0L) {
					// Fast forward through fully empty blocks

					if (contiguousCount == 0) {
						startIndex = i * 64;

						if (i > 0) {
							// if there is a chance to find some free pages at
							// the end of the prev word, lets shift it
							// backwards:
							long prevWord = map[i - 1];
							for (int ii = 63; ii > 0; ii--) {
								/*
								 * the most left bit must be 1 because this if
								 * didnt triggered before, so we iterade [1..63]
								 * in reverse. if ((prevWord & (-1L >>> ii)) ==
								 * 0L) {//check if the last ii bits in prevWord
								 * == 0.
								 */
								if ((prevWord & (1L << ii)) == 0L) {
									startIndex--;// bit is free and we can shift
													// a page backwards.
									contiguousCount++;
								} else {
									break;// bit says its page is occupied and
											// we abbort the search.
									// not an exit condition.
								}
							}
						}

					}

					// while there are empty blocks: step foreward: +64.
					contiguousCount += 64;
					if (contiguousCount >= numPages) {
						return startIndex;// if capacity reached: return offset.
					}
					
					// however, if we find empty blocks, directly search for the next.
					continue;
				}

				for (long j = 0; j < 64; j++) {
					long bitIndex = i * 64 + j;
					if (bitIndex >= maxIndex) {// out-of-bounds? -> file can not fit in this storage.
						// break;
						return -1;
					}
					if ((word & (1L << j)) == 0) {// is page empty?
						if (contiguousCount == 0) {// set start if it is not.
							startIndex = bitIndex;
						}
						contiguousCount++;
						if (contiguousCount >= numPages) {// last page confirmed as empty?
							return startIndex;
						}
					} else {
						//reset start points since there is an interrupting 1 in the current run -> so we discard it.
						startIndex = -1;
						contiguousCount = 0;
					}
				}
			}

			return -1; // No sufficient block found
		}
	}
}