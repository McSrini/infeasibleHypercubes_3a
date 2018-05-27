/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.drivers;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE; 
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.collection.RectangleCollector;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import ca.mcmaster.infeasiblehypercubes_3a.utils.BranchingVariableSuggestor;
import ca.mcmaster.infeasiblehypercubes_3a.utils.Enumerator;
import ca.mcmaster.infeasiblehypercubes_3a.utils.cplex.CplexBasedValidator;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
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
    
    public void solve (int frontierSize){
         
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
            
            if (numLeafsRemaning>=frontierSize) break;
            
            List<LeafNode> bestNodes = this.activeLeafs.get( bestBound);
            long numLeafsRemaningAtThisBound =bestNodes.size();
            LeafNode selectedLeaf =bestNodes.remove(ZERO);
            if (bestNodes.size()==ZERO) {
                this.activeLeafs.remove( bestBound);
            }else {
                this.activeLeafs.put(bestBound, bestNodes);
            }
            System.out.println("best bound is "+ bestBound + " and numleafs is "+ numLeafsRemaningAtThisBound + " out of total " + numLeafsRemaning);
            //System.out.println ("Selected leaf is "+ selectedLeaf + " with lp relax " + selectedLeaf.lpRelaxValueMinimization);
            
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
            
            //  lp vertex of this leaf is better than any compatible rect , then its a feasible leaf
            if ( compatibleRectMap.isEmpty() || 
                (Collections.min(compatibleRectMap.keySet()) > selectedLeaf.lpRelaxValueMinimization )) {
                
                 this.updateIncumbent( selectedLeaf);  
                 //pick next leaf in best first manner
                 continue ; 
            }    
            
            //pick branching var
            VariableCoefficientTuple varWithFreq = BranchingVariableSuggestor.suggest(compatibleRectMap, selectedLeaf);           
            
            /*if ( varWithFreq.coeff==ONE){
                //refcount of every var is 1 , enumerated solution possible, leaf is feasible if vertex found like this is
                //better than best infeasible hypercube
                
                boolean isEnumeratedSolutionFeasible = false;
                LeafNode enumeratedSolution = (new Enumerator (selectedLeaf )) .getEnumeratedSolution(  compatibleRectMap) ;
                if (Collections.min(compatibleRectMap.keySet()) > enumeratedSolution.lpRelaxValueMinimization) {                    
                    isEnumeratedSolutionFeasible = true;
                }                
                
                //pick next leaf in best first manner if this enumerated solution is feasible
                if (isEnumeratedSolutionFeasible) {
                    
                    try {
                        //validate solution using CPLEX
                        CplexBasedValidator validator = new CplexBasedValidator (enumeratedSolution.zeroFixedVariables,
                                enumeratedSolution.lpRelaxValueMinimization);
                    } catch (IloException ex) {
                        System.err.println("Enumerates solution is not valid, error."+ex) ;
                        exit(ONE);
                    }                     
                    
                    this.updateIncumbent( enumeratedSolution );  
                    continue ; //pick up next leaf
                } 
            }*/
                
            //must create 2 child leafs
            this.createChildNodes( selectedLeaf, varWithFreq.varName);
             
            
        }//end while leafs reamin
        
        if(bestKnownSolution==null){
            System.out.println("no feasible solution found for MIP ");
        }else{
            System.out.println("MIP is feasible \n" + bestKnownSolution + " \n best known solution value is = "+ incumbent);
            if (this.activeLeafs.isEmpty()|| getBestBound()>= incumbent) System.out.println("Incumbent is optimal \n" );
        }
        
         
        
    }//end solve
        
    //add leafs in best first manner
    void addActiveLeaf( LeafNode new_Node ) {
        List<LeafNode>  nodeList = this.activeLeafs.get( new_Node.getLpRelaxVertex_Minimization());
        if (nodeList==null) nodeList = new ArrayList<LeafNode> ();
        nodeList.add(new_Node);
        
        this.activeLeafs.put( new_Node.lpRelaxValueMinimization , nodeList);
    }
    
}
