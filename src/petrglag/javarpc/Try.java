package petrglag.javarpc;

import java.util.Arrays;

public class Try {
	public static void main(String[] args) {
		fn(new String[]{"Hello", "world"});
		fn("Hello", "world");
	}
	
	public static void fn(String... objects) {
		System.out.println(Arrays.asList(objects));
	}
}
