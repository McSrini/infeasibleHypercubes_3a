/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a;

/**
 *
 * @author tamvadss
 */
public class Parameters {
       //public static final String MIP_FILENAME = "F:\\temporary files here\\neos-807456.mps";  ////x1 x7  x4  x2
       //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackTinyInfeasible.lp";  ////x1 x7  x4  x2
       //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackSmall.lp";  ////x1 x7  x4  x2
       //public static final String MIP_FILENAME = "F:\\temporary files here\\knapsackFourTest.lp"; 
       //public static final String MIP_FILENAME = "F:\\temporary files here\\stp3d.mps";
       // public static final String MIP_FILENAME = "F:\\temporary files here\\seymour-disj-10.mps";
       //public static final String MIP_FILENAME = "harp2.mps";
       //public static final String MIP_FILENAME = "cov1075.mps";
       //public static final String MIP_FILENAME = "stp3d.mps";
       //public static final String MIP_FILENAME = "seymour-disj-10.mps";
       public static final String MIP_FILENAME = "p6b.mps";
 
       public static boolean USE_STRICT_INEQUALITY_IN_MIP = false;
       
       //for rectangle collection
       public static int INITIAL_RECTS_TO_BE_COLLECTED_PER_CONSTRAINT = 1;   
       public static int REPLENISHMENT_LIMIT = 1;   
       
       public static int NUM_DEPTH_FIRST_DIVES = 100;
       public static int TIME_LIMIT_MINUTES_PER_DEPTH_FIRST_DIVE = 60;
     
}
