/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a;

import ca.mcmaster.infeasiblehypercubes_3a.drivers.Best_First_Driver;
import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;

/**
 *
 * @author tamvadss
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Base_Driver driver = new Best_First_Driver ();
        
                driver.solve();
    }
}
