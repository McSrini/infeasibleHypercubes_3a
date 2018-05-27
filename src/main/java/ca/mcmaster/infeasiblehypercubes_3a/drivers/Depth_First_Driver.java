/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.drivers;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.DOUBLE_ZERO;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.SIXTY_THOUSAND;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.TIME_LIMIT_MINUTES_PER_DEPTH_FIRST_DIVE;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import static ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver.logger;
import ca.mcmaster.infeasiblehypercubes_3a.utils.BranchingVariableSuggestor;
import ca.mcmaster.infeasiblehypercubes_3a.utils.Enumerator;
import ca.mcmaster.infeasiblehypercubes_3a.utils.cplex.CplexBasedValidator;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 * 
 * dive into branch rooted at seed, try to find solutions for a few minutes
 * 
 */
public class Depth_First_Driver extends Base_Driver{
    
    private LeafNode seed = null;
 
    public Depth_First_Driver (LeafNode seed) throws IloException {
        super () ;
        this.seed= seed;
    }
 
    public void solve(int frontierSize) {
        
        //init 
        this.addActiveLeaf(seed);
        
        Instant startTime = Instant.now();
        
        while (activeLeafs.size()>ZERO  ){
            
            //printAllLeafs();
            
            double timeUsedUpMInutes = ( DOUBLE_ZERO+ Duration.between( startTime, Instant.now()).toMillis() ) / (SIXTY_THOUSAND) ;
            if (timeUsedUpMInutes > TIME_LIMIT_MINUTES_PER_DEPTH_FIRST_DIVE) {
                logger.info("Dive timeout reached for leaf "+seed) ;
                break;
            }
            
            //pick leaf with lowest reamining number of comaptible infeasible rectangles
            double lowRectCount = getBestBound();
            List<LeafNode> bestNodes = this.activeLeafs.get( lowRectCount);
            LeafNode selectedLeaf =bestNodes.remove(ZERO);
            if (bestNodes.size()==ZERO) {
                this.activeLeafs.remove( lowRectCount);
            }else {
                this.activeLeafs.put(lowRectCount, bestNodes);
            }
            
            
            try {
                //make sure all infeasible rectangles are available, whose LP is better than selected node                
                logger.debug ("Number of rects replenished by this leaf = " + this.replenishRectanglesAll( selectedLeaf.lpRelaxValueMinimization));
            } catch (IloException ex) {
                logger.error("unable to refresh rects "+ex) ;
                exit(ONE);
            }
            
            //get the compatible rectangles for this selectedLeaf
            Map<Double, List<Rectangle>  > compatibleRectMap = new TreeMap<Double, List<Rectangle>  > ();
            boolean isInfeasible = this.getCompatibleRectanglesFromEveryConstraint( selectedLeaf, compatibleRectMap );
            
            //we now have the leaf with the lowest reamining rect count
            System.out.println("best bound is "+  selectedLeaf.lpRelaxValueMinimization + 
                               " selected leaf "+ selectedLeaf.myId +" has this many rects to eliminate" + getNumberOfRects( compatibleRectMap, selectedLeaf.lpRelaxValueMinimization));
            logger.debug("best bound is "+  selectedLeaf.lpRelaxValueMinimization + 
                               " selected leaf "+ selectedLeaf.myId +" has this many rects to eliminate" + getNumberOfRects( compatibleRectMap, selectedLeaf.lpRelaxValueMinimization));
            System.out.flush();
            
            if (isInfeasible) {
                //simply discard this leaf, and pick next leaf
                logger.debug("selected leaf was infeasible, discarding ...") ;
                System.out.println("selected leaf was infeasible, discarding ...") ;
                System.out.flush(); 
                continue; 
            }  
             
            //  lp vertex of this leaf is better than any compatible rect , then its a feasible leaf
            if ( compatibleRectMap.isEmpty() || 
                (Collections.min(compatibleRectMap.keySet()) > selectedLeaf.lpRelaxValueMinimization )) {
                
                //logger.info("feasible vertex found") ;
                this.validateSolutionWithCplex(selectedLeaf);
                    
               
                this.updateIncumbent( selectedLeaf);  
                //pick next leaf  
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
                    
                    this.validateSolutionWithCplex(enumeratedSolution);
                    logger.info("enumerated solution found") ;
                    this.updateIncumbent( enumeratedSolution );  
                    continue ; //pick up next leaf
                } 
            }*/
                
            //must create 2 child leafs
            this.createChildNodes( selectedLeaf, varWithFreq.varName);
            
        }//end while
                 
    }//end solve
    
    private void validateSolutionWithCplex (LeafNode candidateSolution) {
        try {
            //validate solution using CPLEX
            CplexBasedValidator validator= new CplexBasedValidator (candidateSolution.zeroFixedVariables,
                    candidateSolution.lpRelaxValueMinimization);
            if (!validator.isValid()) {
                System.err.println("proposed solution is not valid, error.") ;
                exit(ONE);
            } else {
                logger.debug("proposed Solution validted by cplex") ;
            }
        } catch (IloException ex) {
            System.err.println("proposed solution is not valid, error."+ex) ;
            exit(ONE);
        }   
    }
    
    //key is number of compatible rects having same LP as best vertex
    void addActiveLeaf( LeafNode new_Node ) {
        
        new_Node.getLpRelaxVertex_Minimization();
        
        Map<Double, List<Rectangle>  > compatibleRectMap = new TreeMap<Double, List<Rectangle>  > ();
        boolean    isInfeasible = this.getCompatibleRectanglesFromEveryConstraint( new_Node, compatibleRectMap );
         
        Double numOfCompatibleRects = DOUBLE_ZERO+ getNumberOfRects(compatibleRectMap , new_Node.lpRelaxValueMinimization )      ;
        List<LeafNode>  nodeList = this.activeLeafs.get( numOfCompatibleRects);
        if (nodeList==null) nodeList = new ArrayList<LeafNode> ();
        nodeList.add(new_Node);
        
        if (!  isInfeasible ) this.activeLeafs.put( numOfCompatibleRects , nodeList);
    }
}
