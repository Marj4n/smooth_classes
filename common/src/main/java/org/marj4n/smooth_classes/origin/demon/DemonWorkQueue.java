package org.marj4n.smooth_classes.origin.demon;

import java.util.*;

/** Uncapped fair loaded-entity work queue; changes never restart iteration at the first mob. */
public final class DemonWorkQueue<K,V> {
    private final Map<K,V> values=new HashMap<>();
    private final ArrayDeque<K> queue=new ArrayDeque<>();
    private final Set<K> queued=new HashSet<>();
    public void put(K key,V value){values.put(key,value);if(queued.add(key))queue.add(key);}
    public void remove(K key){values.remove(key);if(values.isEmpty())clear();}
    public void remove(K key,V expected){if(values.get(key)==expected)remove(key);}
    public int size(){return values.size();}
    public V get(K key){return values.get(key);}
    public V next(){
        while(!queue.isEmpty()){
            K key=queue.remove();V value=values.get(key);
            if(value==null){queued.remove(key);continue;}
            queue.add(key);return value;
        }
        return null;
    }
    public void clear(){values.clear();queued.clear();queue.clear();}
}
