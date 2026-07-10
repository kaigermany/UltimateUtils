package me.kaigermany.ultimateutils.special;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public class UnknownResourceInjector9 {
	@SuppressWarnings("unchecked")
	public static void injectForJava9() throws Exception {
		System.out.println("init:injectForJava9");
		HashSet<Object> knownModuleDescriptors = new HashSet<>(256);
		Class<?> bclType = Class.forName("jdk.internal.loader.BuiltinClassLoader");
		Class<?> moduleReferenceClass = Class.forName("java.lang.module.ModuleReference");

		final Map<String, Object> packageToModule = (Map<String, Object>)Unsafe.getStaticField(bclType, bclType.getDeclaredField("packageToModule"));

		for(Object loadedModule : packageToModule.values()){
			Object moduleReference = Unsafe.getDynamicField(loadedModule, loadedModule.getClass().getDeclaredField("mref"));
			Object moduleDescriptor = Unsafe.getDynamicField(moduleReference, moduleReferenceClass.getDeclaredField("descriptor"));

			Unsafe.setDynamicField(moduleDescriptor, moduleDescriptor.getClass().getDeclaredField("open"), true);

			knownModuleDescriptors.add(moduleDescriptor);
		}

		Field nameToModuleRawPointer = bclType.getDeclaredField("nameToModule");
		Field moduleToReaderRawPointer = bclType.getDeclaredField("moduleToReader");

		Class<?> ClassLoadersClazz = Class.forName("jdk.internal.loader.ClassLoaders");

		for(String bootLoaderName : new String[]{"BOOT_LOADER", "PLATFORM_LOADER", "APP_LOADER"}) {
			Object BuiltinClassLoaderInstance = Unsafe.getStaticField(ClassLoadersClazz, ClassLoadersClazz.getDeclaredField(bootLoaderName));

			final Map<String, Object> nameToModule = (Map<String, Object>)Unsafe.getDynamicField(BuiltinClassLoaderInstance, nameToModuleRawPointer);

			for (Object moduleReference : nameToModule.values()) {
				Object moduleDescriptor = Unsafe.getDynamicField(moduleReference, moduleReferenceClass.getDeclaredField("descriptor"));

				Unsafe.setDynamicField(moduleDescriptor, moduleDescriptor.getClass().getDeclaredField("open"), true);

				knownModuleDescriptors.add(moduleDescriptor);
			}
			//ModuleReference, ModuleReader
			Map<Object, Object> moduleToReader = (Map<Object, Object>)Unsafe.getDynamicField(BuiltinClassLoaderInstance, moduleToReaderRawPointer);
			final Class<?> mr = Class.forName("java.lang.module.ModuleReader");
			Unsafe.setDynamicField(BuiltinClassLoaderInstance, moduleToReaderRawPointer,
					new GetInjectedMap<>(moduleToReader,
							new BiFunction<Object, Object, Object>(){
								private final HashMap<Object, Object> localCacheMap = new HashMap<>(128);
								@Override
								public Object apply(Object key, Object value) {
									System.out.println("GetInjectedMap::get");
									System.out.println("value = " + value);
									//if (value == null) return null;
									Object out;
									synchronized (localCacheMap) {
										out = localCacheMap.computeIfAbsent(value, k -> {
											//new WrappedModuleReader(k, packageToModule, (ModuleReference)key)
											Object a = wrapModuleReader(k, packageToModule, key, mr);
											System.out.println(a.getClass().getName());
											System.out.println(a.getClass().getSuperclass().getName());
											System.out.println(a.getClass().getSuperclass().getSuperclass().getName());
											return a;
										}
										);
									}
									return out;
								}
							}
					)
			);//injection confirmed.
		}

		for(Object md : knownModuleDescriptors){
			try{//if not used, randomly errors will occur! idk why...
				Unsafe.setDynamicField(md, md.getClass().getDeclaredField("open"), true);

				Set<String> packagesSet = (Set<String>)Unsafe.getDynamicField(md, md.getClass().getDeclaredField("packages"));
				Unsafe.setDynamicField(md, md.getClass().getDeclaredField("packages"), new SetButEveryContainsIsTrue<>(packagesSet));//CONFIRMED


			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	public static class SetButEveryContainsIsTrue<T> implements Set<T> {
		private final Set<T> src;
		public SetButEveryContainsIsTrue(Set<T> src){
			this.src = src;
		}

		@Override
		public int size() {
			return src.size();
		}

		@Override
		public boolean isEmpty() {
			return src.isEmpty();
		}

		@Override
		public boolean contains(Object o) {//let's say we are god and know every single object :)
			return true;
		}

		@Override
		public Iterator<T> iterator() {
			return src.iterator();
		}

		@Override
		public Object[] toArray() {
			return src.toArray();
		}

		@Override
		public <T2> T2[] toArray(T2[] a) {
			return src.toArray(a);
		}

		@Override
		public boolean add(T s) {
			return src.add(s);
		}

		@Override
		public boolean remove(Object o) {
			return src.remove(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return src.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			return src.addAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return src.retainAll(c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return src.removeAll(c);
		}

		@Override
		public void clear() {
			src.clear();
		}
	}
	public static class GetInjectedMap<K,V> implements Map<K, V> {
		private final Map<K, V> src;
		private final BiFunction<Object, V, V> getterFunction;
		public GetInjectedMap(Map<K, V> src, BiFunction<Object, V, V> getterFunction){
			this.src = src;
			this.getterFunction = getterFunction;
		}
		@Override
		public int size() {
			return src.size();
		}

		@Override
		public boolean isEmpty() {
			return src.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return src.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return src.containsValue(value);
		}

		@Override
		public V get(Object key) {//lets overtake the get() by injecting our custom method.
			System.out.println("get()");
			return getterFunction.apply(key, src.get(key));
		}

		@Override
		public V put(K key, V value) {
			return src.put(key, value);
		}

		@Override
		public V remove(Object key) {
			return src.remove(key);
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			src.putAll(m);
		}

		@Override
		public void clear() {
			src.clear();
		}

		@Override
		public Set<K> keySet() {
			return src.keySet();
		}

		@Override
		public Collection<V> values() {
			return src.values();
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return src.entrySet();
		}
	}

	public static Object wrapModuleReader(final Object readerInput, final Map<String, Object> nameToModule, final Object moduleSource, final Class<?> mr) {
		if(readerInput == null){
			return Proxy.newProxyInstance(mr.getClassLoader(), new Class[]{mr}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					
					System.out.println(method);
					
					if(method.getName().equals("open")){
						String name = (String)args[0];
						InputStream is = UnknownResourceListener.getResource(reverseResolveName(name, nameToModule, moduleSource));
						if(is != null) {
							return Optional.of(is);
						}
						return Optional.empty();
					} else if(method.getName().equals("find")){
						String name = (String)args[0];
						InputStream is = UnknownResourceListener.getResource(reverseResolveName(name, nameToModule, moduleSource));
						if(is != null) {
							try {
								return Optional.of(UnknownResourceListener.createURL(is).toURI());//toURI() loses the custom handler, so we need also to implement open()-method to compete a request.
							} catch (Exception ignored) {}
						}
						return Optional.empty();
					} else if(method.getName().equals("list")){
						return null;
					} else if(method.getName().equals("close")){
						return null;
					} else {
						method.invoke(null, args);
					}
					return null;
				}
			});
		}
		return Proxy.newProxyInstance(/*UnknownResourceInjector9.class.getClassLoader()*/mr.getClassLoader(), new Class[]{mr}, new InvocationHandler() {
			@SuppressWarnings("unchecked")
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				System.out.println(method);
				
				if(method.getName().equals("open")){
					String name = (String)args[0];
					Optional<InputStream> outDefault = (Optional<InputStream>)readerInput.getClass().getDeclaredMethod("open", String.class).invoke(readerInput, args);//readerInput.open(name);
					if(!outDefault.isPresent()){
						InputStream is = UnknownResourceListener.getResource(reverseResolveName(name, nameToModule, moduleSource));
						if(is != null) {
							return Optional.of(is);
						}
					}
					return outDefault;
				} else if(method.getName().equals("find")){
					String name = (String)args[0];
					{
						Optional<URI> outDefault = (Optional<URI>)readerInput.getClass().getDeclaredMethod("find", String.class).invoke(readerInput, args);//readerInput.find(name);
						if(outDefault.isPresent()){
							return outDefault;
						}
					}
					InputStream is = UnknownResourceListener.getResource(reverseResolveName(name, nameToModule, moduleSource));
					if(is != null) {
						try {
							return Optional.of(UnknownResourceListener.createURL(is).toURI());//toURI() loses the custom handler, so we need also to implement open()-method to compete a request.
						} catch (Exception ignored) {}
					}
					return Optional.empty();
				} else if(method.getName().equals("list")){
					return readerInput.getClass().getDeclaredMethod("list").invoke(readerInput);
				} else if(method.getName().equals("close")){
					return readerInput.getClass().getDeclaredMethod("close").invoke(readerInput);
				} else {
					method.invoke(null, args);
				}
				return null;
			}
		});
	}

	private static String reverseResolveName(String name, Map<String, Object> nameToModule, Object moduleSource){
		try{
			for(Map.Entry<String, Object> e : nameToModule.entrySet()){//a module can contain multiple package paths!
				Object moduleReference = Unsafe.getDynamicField(e.getValue(), e.getValue().getClass().getDeclaredField("mref"));
				if(moduleReference == moduleSource){//filter all packages that can not contain our path
					String prefix = e.getKey().replace('.', '/') + '/';
					//System.out.println("prefix = " + prefix);
					if(name.startsWith(prefix)){
						return name.substring(prefix.length());
					}
				}
			}
		}catch (Exception e) {e.printStackTrace();}

		return name;
	}
}
