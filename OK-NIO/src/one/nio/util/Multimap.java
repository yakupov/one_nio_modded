package one.nio.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Multimap<K, V> {
	protected ConcurrentMap<K, List<V>> map;
	
	public Multimap() {
		map = new ConcurrentHashMap<K, List<V>>();
	}
	
	public void put(K key, V value) {
		if (!map.containsKey(key)) {
			map.put(key, Collections.synchronizedList(new ArrayList<V>()));
		}
		List<V> vals = map.get(key);
		vals.add(value);
	}
	
	public Collection<V> get(K key) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		return null;
	}
	
	public V getRandom(K key) {
		if (map.containsKey(key)) {
			List<V> vals = map.get(key);
			int size = vals.size();
			
			return vals.get((int) Math.round(Math.random() * size));
		}
		return null;
	}
}
