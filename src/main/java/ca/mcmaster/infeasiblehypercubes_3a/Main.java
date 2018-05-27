/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.RectangleCollector;
import ca.mcmaster.infeasiblehypercubes_3a.drivers.Best_First_Driver;
import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;
import ca.mcmaster.infeasiblehypercubes_3a.drivers.Depth_First_Driver;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class Main {
       
    private static Logger logger=Logger.getLogger(Main.class);
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+Main.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        //solve in best first manner till we have 100 leafs
        Base_Driver driver = new Best_First_Driver ();
        driver.solve(  Parameters.NUM_DEPTH_FIRST_DIVES);
        
        //get the ramped up leafs
        List<LeafNode> leafsFromRampUp = new ArrayList<LeafNode> ();
        for ( List<LeafNode> leafList :driver.activeLeafs.values())        {
            leafsFromRampUp.addAll(leafList);
        }
                
        //dive into each leaf
        for (LeafNode rampedUpLeaf: leafsFromRampUp) {
            Base_Driver depthDiver = new Depth_First_Driver(rampedUpLeaf);
            depthDiver.solve( Integer.MAX_VALUE);
            logger.debug("best solution found by this dive is =" +depthDiver.incumbent);
        }
               
    }
}
