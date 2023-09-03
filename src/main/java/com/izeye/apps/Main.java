package com.izeye.apps;

import java.util.Scanner;

/**
 * Main class.
 *
 * @author Johnny Lim
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("사과");
        String line;
        while ((line = new Scanner(System.in).nextLine()) != null) {
            if (line.equals("apple")) {
                break;
            }
            System.out.println("Try again!");
        }
        System.out.println("Congratulation!");
    }

}
