package me.brook.Snake;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputManager implements KeyListener {

	public boolean[] buttons = new boolean[1000];

	public void reset() {
		buttons = new boolean[1000];
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		int code = arg0.getKeyCode();
//		 System.out.println(code);
		if(code >= buttons.length) {
			System.out.printf("Button code '%s' is outside of range.\n", code);
		}
		buttons[code] = true;
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

}
