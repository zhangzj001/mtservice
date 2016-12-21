package cn.jugame.ms;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class Test2 {
	public static Unsafe getUnsafe() {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (Unsafe) f.get(null);
		} catch (Exception e) {
			/* ... */
			return null;
		}
	}
	
	public static void main(String[] args) {
		Unsafe unsafe = getUnsafe();
		long handle = unsafe.allocateMemory(1024);
		unsafe.freeMemory(handle);
	}
}
