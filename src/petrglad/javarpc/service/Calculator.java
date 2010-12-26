package petrglad.javarpc.service;

import java.util.Random;

public class Calculator {

	Random random = new Random();

	public Long add(Long a, Long b) {
		return a + b;
	}

	public Long guess(Long a, Long b) {
		try {
			Thread.currentThread().sleep(0, random.nextInt(999999));
		} catch (InterruptedException e) {
		}
		return a * b;
	}
}
