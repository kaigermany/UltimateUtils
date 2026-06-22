package me.kaigermany.ultimateutils.data.generictables;
/*
 * UncheckedBitMap simply wraps an int[] and use it to set or unset single bits on it.
 * to ensure perfect performance, there are no boundary checks at all, usage at own risk.
 * If you want or need boundary checks, then prefer to use BitMap.java instead.
 */
public class UncheckedBitMap {
	private final int[] map;

	public UncheckedBitMap(int size){
		map = new int[(size + 31) / 32];
	}
	public UncheckedBitMap(UncheckedBitMap copyFrom){
		map = copyFrom.map.clone();
	}
	public UncheckedBitMap(UncheckedBitMap copyFrom, int newSize){
		map = new int[(newSize + 31) / 32];
		System.arraycopy(copyFrom.map, 0, map, 0, Math.min(copyFrom.map.length, map.length));
	}
	
	public int[] getDirectMapAccess(){
		return map;
	}
	
	public int get(int index){
		return (map[index >> 5] >> (index & 31)) & 1;
	}
	
	public void set(int index, int bit){//bit MUST be 0 or 1
		//map[index >> 5] |= bit << (index & 31);
		//map[index >> 5] &= ~(~bit << (index & 31));
		//BUG reported: map[index >> 5] = ( map[index >> 5] | (bit << (index & 31)) ) & ~(~bit << (index & 31));
		map[index >> 5] = ( map[index >> 5] & ~(1 << (index & 31)) ) | (bit << (index & 31));
	}
	
	public void set(int index, boolean bit){
		if(bit){
			map[index >> 5] |= 1 << (index & 31);
		} else {
			map[index >> 5] &= ~(1 << (index & 31));
		}
	}
	
	public void fill(int start, int length, boolean bit){
		if(length == 0) return;
		if(bit){
			int currentWord = map[start >> 5];
			//fill the first int until full or length-underflow.
			while((start & 31) != 0){
				currentWord |= 1 << (start & 31);
				if(--length == 0){
					//if length falls to zero, we are done.
					map[start >>= 5] = currentWord;
					return;
				}
				start++;
			}
			//now 'start' is exactly at the head of the next int.
			//then we scale to int-space, and seek to the 'previous' entry.
			map[(start >>= 5) - 1] = currentWord;
			//start now stays on int-space!
			
			while(length >= 32){
				map[start] = -1;
				start++;
				length -= 32;
			}
			
			currentWord = map[start];
			start <<= 5;//start now back on bit-space!
			while(length != 0){
				currentWord |= 1 << (start & 31);
				start++;
				length--;
			}
			map[start >> 5] = currentWord;
		} else {
			int currentWord = map[start >> 5];
			while((start & 31) != 0 && length != 0){
				currentWord &= ~(1 << (start & 31));
				if(--length == 0){
					//if length falls to zero, we are done.
					map[start >>= 5] = currentWord;
					return;
				}
				start++;
			}
			map[start >>= 5] = currentWord;
			//start now on int-space!
			while(length >= 32){
				map[start] = 0;
				start++;
				length -= 32;
			}
			currentWord = map[start];
			start <<= 5;//start now back on bit-space!
			while(length != 0){
				currentWord &= ~(1 << (start & 31));
				start++;
				length--;
			}
			map[start >> 5] = currentWord;
		}
	}
	
	/*
	public void testFill() {
	    UncheckedBitMap bm = new UncheckedBitMap(100);
	    // Fill middle bits
	    bm.fill(30, 40, true);
	    for (int i = 30; i < 70; i++) {
	        assertEquals(1, bm.get(i));
	    }
	    // Fill across word boundary
	    bm.fill(29, 40, false);
	    for (int i = 29; i < 69; i++) {
	        assertEquals(0, bm.get(i));
	    }
	    // Fill zero bits
	    bm.fill(0, 0, true);
	    // Fill at end
	    bm.fill(95, 5, true);
	    for (int i = 95; i < 100; i++) {
	        assertEquals(1, bm.get(i));
	    }
	}
	private static void assertEquals(int a, int b){
		if(a != b) throw new RuntimeException(a + " != " + b);
	}
	*/
}
