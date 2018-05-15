/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.drivers;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE;
import static ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver.getBestBound;
import static ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver.incumbent;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.collection.RectangleCollector;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import ca.mcmaster.infeasiblehypercubes_3a.utils.BranchingVariableSuggestor;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tamvadss
 */
public class Best_First_Driver extends Base_Driver {
    
    public Best_First_Driver () throws IloException {
        super () ;
    }
    
    public void solve (){
         
        //init
        LeafNode root = new LeafNode (new ArrayList<String> (), new ArrayList<String> ()) ;
        this.addActiveLeaf(root);
        
        //pick best leaf repeatedly
        while (activeLeafs.size()>ZERO && getBestBound()< incumbent){
            
            //pick the best LP leaf
            printAllLeafs() ;    
            //pluck the best reamaining leaf
            double bestBound = getBestBound();
            long numLeafsRemaning = getNumberOfLeafs(this.activeLeafs);
            List<LeafNode> bestNodes = this.activeLeafs.get( bestBound);
            LeafNode selectedLeaf =bestNodes.remove(ZERO);
            if (bestNodes.size()==ZERO) {
                this.activeLeafs.remove( bestBound);
            }else {
                this.activeLeafs.put(bestBound, bestNodes);
            }
            System.out.println(" \n\nbest bound is "+ bestBound + " incumbent is " +incumbent + " numleafs is "+ numLeafsRemaning);
            System.out.println ("Selected leaf is "+ selectedLeaf + " with lp relax " + selectedLeaf.lpRelaxValueMinimization);
            
            try {
                //make sure all infeasible rectangles are available, whose LP is better than selected node
                
                logger.debug ("Number of rects replenished by this leaf = " + this.replenishRectanglesAll(bestBound));
            } catch (IloException ex) {
                logger.error("unable to refresh rects "+ex) ;
                exit(ONE);
            }
            
            //get the compatible rectangles for this selectedLeaf
            Map<Double, List<Rectangle>  > compatibleRectMap = new TreeMap<Double, List<Rectangle>  > ();
            boolean isInfeasible = this.getCompatibleRectanglesFromEveryConstraint( selectedLeaf, compatibleRectMap );
            
            if (isInfeasible) {
                //simply discard this leaf, and pick next leaf
                logger.debug("selected leaf was infeasible, discarding ...") ;
                continue;
            }   
            
            //pick branching var
            VariableCoefficientTuple varWithFreq = BranchingVariableSuggestor.suggest(compatibleRectMap, selectedLeaf);
            
            if (false) {
                //  lp vertex of this is better than any compatible rect => feasible leaf
            }    else if (false){
                //refcount of every var is 1 , enumerated solution possible, leaf is feasible
            }else {
                //must create 2 child leafs
                this.createChildNodes( selectedLeaf, varWithFreq.varName);
            }
            
        }//end while leafs reamin
        
        if(bestKnownSolution==null){
            System.out.println("MIP is infeasible");
        }else{
            System.out.println("MIP is optimal \n" + bestKnownSolution + " \n optimal value is = "+ incumbent);
        }
        
        logger.debug("Priniting all collected rects") ;
        for (RectangleCollector collector : this.rectangleCollectorList){
            //collector.printAllCollectedRects();
        }
        
    }//end solve
        
   
    
}
