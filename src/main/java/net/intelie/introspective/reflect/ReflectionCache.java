package net.intelie.introspective.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ReflectionCache {
    private final Map<Class<?>, Item> cache;
    private final Predicate<Field> shouldFollow;

    public ReflectionCache() {
        this(x -> true);
    }

    public ReflectionCache(Predicate<Field> shouldFollow) {
        this.shouldFollow = shouldFollow;
        this.cache = new IdentityHashMap<>();
    }

    public Item get(Class<?> clazz) {
        Item item = cache.get(clazz);
        if (item == null)
            cache.put(clazz, item = new Item(clazz));
        return item;
    }

    public class Item {
        private final long size;
        private final FastFieldAccessor[] fields;

        public Item(Class<?> clazz) {
            List<FastFieldAccessor> peelable = new ArrayList<>();

            long size = JVMPrimitives.getObjectHeaderSize();
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()))
                        continue;

                    size = Math.max(size, JVMPrimitives.getFieldOffset(field) + JVMPrimitives.getPrimitive(field.getType()));
                    if (field.getType().isPrimitive() || !shouldFollow.test(field))
                        continue;
                    peelable.add(new FastFieldAccessor(field));
                }
                clazz = clazz.getSuperclass();
            }

            this.size = size;
            this.fields = peelable.toArray(new FastFieldAccessor[0]);
        }

        public long size() {
            return size;
        }

        public int fieldCount() {
            return fields.length;
        }

        public String fieldName(int index) {
            return fields[index].name();
        }

        public Object value(Object target, int index) {
            return fields[index].get(target);
        }
    }
}