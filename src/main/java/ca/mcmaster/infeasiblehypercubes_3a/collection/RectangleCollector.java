/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.collection;

import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.*;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.INITIAL_RECTS_TO_BE_COLLECTED_PER_CONSTRAINT;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.REPLENISHMENT_LIMIT;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.infeasiblehypercubes_3a.common.*;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.*;
import java.util.Map.Entry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/** 
 * 
 * @author tamvadss
 * 
 * *collects rectangles for a constraint
 * 
 */
public class RectangleCollector {
    
    public   Map<Double, List<Rectangle>  > collectedFeasibleRectangles = new TreeMap <Double, List<Rectangle>>  ();    
    
    //this is the  complimented constraint for which we will collect rects
    private UpperBoundConstraint ubc ;
    
    //here are the nodes which are by products of createing the feasible node. These need to be
    //decomposed further to get more feasible nodes
    private    Map<Double, List<Rectangle>  > pendingJobs = new TreeMap <Double, List<Rectangle>>  ();  
    
    private static Logger logger=Logger.getLogger(RectangleCollector.class);
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+RectangleCollector.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
    }
    
     
    public RectangleCollector (  LowerBoundConstraint lbc) throws IloException{
        
        this.ubc=(new UpperBoundConstraint(  lbc)) ;
        
        
        //init        
        Rectangle emptyRect = new Rectangle (  new ArrayList <String>(),  new ArrayList <String>());
        this.addPendingJob(emptyRect);
        
        
        
    }
    
    
    //collect until best reamining pending job has higher LP than specified threshold
    public int replenishRectangles  (double lp_threshold) throws IloException {
        logger.debug("\n replenishRectangles to threshold  ... "+lp_threshold);
        int count = ZERO;
        int iterations = ZERO;
        while (!this.pendingJobs.isEmpty()){
             
            double bestLPRelax = Collections.min( pendingJobs.keySet());
            if (bestLPRelax> lp_threshold)break;
            
            if ( collectOne ()) count ++;
            
            iterations++;
                    
            //System.out.println("Collection iterations "+ iterations) ;
            if (REPLENISHMENT_LIMIT<iterations && count > ZERO )  break;
             
        }
        logger.debug("\n replenishRectangles ended , collected this many  ... "+count);
        return count;
    }
    
    //collect  1 more
    public boolean collectOne () throws IloException {
        //
        logger.debug("\nCollecting One feasible hypercube ... ");
        boolean isCollected = false;
        while (!this.pendingJobs.isEmpty()){
            printAllPendingJobs();
           
            double bestLPRelax = Collections.min( pendingJobs.keySet());
            List<Rectangle> bestLPJobs = pendingJobs.get(bestLPRelax);
            Rectangle job = bestLPJobs.remove(ZERO);
            if (bestLPJobs.size()==ZERO) {
                pendingJobs.remove(bestLPRelax);
            }else {
                pendingJobs.put(bestLPRelax, bestLPJobs);
            }
           
            if (decompose(job)) {
                //1 collected
                isCollected= true;
                break;
            }
            //else repeat while loop
        }
        return isCollected;
    }
      
    public void printAllPendingJobs() {
        if (this.pendingJobs.isEmpty()) logger.debug("no pending jobs for " + this.ubc.name)  ; else logger.debug("printing pending jobs for " + this.ubc.name);
        for (Entry<Double,List<Rectangle>> entry   : this.pendingJobs.entrySet()){
            logger.debug("lp relax is "+entry.getKey()); 
            for (Rectangle  job: entry.getValue()){
                logger.debug(job);
            }
        }
    }
        
    public void printAllCollectedRects() {
        if (this.collectedFeasibleRectangles.isEmpty()) logger.debug("no collected rects for "+ this.ubc.name )  ; else logger.debug("printing collected rects " + this.ubc.name);
        for (Entry<Double,List<Rectangle>> entry   : this.collectedFeasibleRectangles.entrySet()){
            logger.debug("lp relax is "+entry.getKey()); 
            for (Rectangle  job: entry.getValue()){
                logger.debug(job);
            }
        }
    }
     
    //decompose a job and add feasible node found into collection, and more jobs into joblist
    private boolean decompose (Rectangle job) throws IloException {
        
        boolean isOneRectangleCollected = true;
        
        logger.debug("decomposing job "+ job);
        
        UpperBoundConstraint reducedConstraint = ubc.getReducedConstraint(job.zeroFixedVariables, job.oneFixedVariables);
        
        //if reduced constraint is trivially feasible or unfeasible, no need for dempostion
        if (reducedConstraint.isTrivially_Feasible( !USE_STRICT_INEQUALITY_IN_MIP) || reducedConstraint.isGauranteedFeasible(!USE_STRICT_INEQUALITY_IN_MIP)) {
            this.addLeafToFeasibleCollection(job);
            logger.debug( "Collected whole feasible rect "+job);
        }else if (reducedConstraint.isTrivially_Infeasible( !USE_STRICT_INEQUALITY_IN_MIP) || reducedConstraint.isGauranteed_INFeasible(!USE_STRICT_INEQUALITY_IN_MIP) ){
            //discard
            logger.debug("discard infeasible rect " + job);
            isOneRectangleCollected= false;
        }else {
            //must decompose    
            //idea is to get a large feasible hypercube starting from the best(aka origin) vertex
            //
            //for each variable in the constraint, starting with the lowest coeff, see if flipping its value from its origin value will render constraint infeasible
            //flip only those vars  so that flip will increase objective value
            // 
            //delta is how much we move the constraint from the origin by flipping this variable 
            //             
            double delta =ZERO;
            List<Integer> indexOfVarsWhichCanBeFree = new ArrayList<Integer> () ;
             
            for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++){   
                VariableCoefficientTuple tuple= reducedConstraint.sortedConstraintExpr.get(index);
                double objCoeff = Base_Driver.objective.getObjectiveCoeff(tuple.varName);                 
                double changeInObjectiveIfFlipped = job.isVariableFixedAtZeroAtBestVertex.get(index) ?  objCoeff : -objCoeff;
                
                //only consider those vars for flipping, which will result in objective value getting increased
                if (changeInObjectiveIfFlipped<=ZERO) continue; //maybe we can flip the next var
                
                double changeInConstarintValueIfFlipped = job.isVariableFixedAtZeroAtBestVertex.get(index) ?   tuple.coeff : -tuple.coeff ;
                boolean isConstraintViolated = !USE_STRICT_INEQUALITY_IN_MIP ? 
                        (delta +  changeInConstarintValueIfFlipped  +job.reducedConstraint_ValueAtBestVertex>= reducedConstraint.upperBound): 
                        (delta +  changeInConstarintValueIfFlipped  +job.reducedConstraint_ValueAtBestVertex> reducedConstraint.upperBound);
                
                if ( !isConstraintViolated) {
                    //we can flip this var                     
                    indexOfVarsWhichCanBeFree.add(index) ;
                    //delta increases by flipping      
                    delta += changeInConstarintValueIfFlipped ;
               
                    logger.debug("Free var " + tuple.varName) ;
                    //System.out.println("tuple added "+ tuple.var.name);
                }
                
            }//end for
                        
            //now we know the vars which can be free.  
            Rectangle feasibleLeaf = createFeasibleRectangle (job ,indexOfVarsWhichCanBeFree , reducedConstraint) ;
            this.addLeafToFeasibleCollection(feasibleLeaf );
            
            List<Rectangle> newJobs = null;
            if ( indexOfVarsWhichCanBeFree.size()!=ZERO) {
                newJobs = createMoreNodesForDecompostion (job,  indexOfVarsWhichCanBeFree , reducedConstraint) ;
                this.addPendingJobs(newJobs);  
            }
                        
            logger.debug( "Collected feasible rect "+feasibleLeaf + " having best vertex value " + feasibleLeaf.bestVertexValue);
            
        }//end else must decompose
        
        return isOneRectangleCollected;
    }//end method decompose
        
    private List<Rectangle> createMoreNodesForDecompostion (Rectangle job,                                                            
                                                          List<Integer> indexOfVarsWhichCanBeFree ,
                                                          UpperBoundConstraint reducedConstraint ) {
        
        List<Rectangle> newJobs = new ArrayList<Rectangle>();
        
        
        //find the vars which cannot be flipped
        List<Integer> indexOfVarsWhichAreNotFree  = new ArrayList<Integer>   ();
        for (Integer index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++) {
            if (! indexOfVarsWhichCanBeFree.contains(index)) indexOfVarsWhichAreNotFree.add(index) ;
        }
        
        //starting with the highest coeff var in the reduced constraint, flip var value from origin, and for all higher coeff vars retain their value at origin
        //# of jobs created = (# of freevars in reduced constr - countOfVarsWhichCanBeFree)
        
        
        for (int jobIndex = ZERO;  
                jobIndex< indexOfVarsWhichAreNotFree.size() ;
                jobIndex++){
            Rectangle  newJob = new Rectangle(job.zeroFixedVariables, job.oneFixedVariables ) ;
            
            //add flipped  branching condition for jth largest var
            int size =  indexOfVarsWhichAreNotFree.size();
            int thisVarIndex = indexOfVarsWhichAreNotFree.get(size-ONE-jobIndex );
                    
            if (job.isVariableFixedAtZeroAtBestVertex.get(thisVarIndex)){
                newJob.oneFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(thisVarIndex).varName);
            }else {
                newJob.zeroFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(thisVarIndex).varName);
            }
            
            //for all the other higher coeff vars, add branching conditions by using their value at origin
            int numberOfHigherCoeffVars = jobIndex;
            for (; numberOfHigherCoeffVars >ZERO; numberOfHigherCoeffVars--){
                
                thisVarIndex = indexOfVarsWhichAreNotFree.get(size- numberOfHigherCoeffVars);
                        
                if (job.isVariableFixedAtZeroAtBestVertex.get( thisVarIndex)) {
                    newJob.zeroFixedVariables.add(reducedConstraint.sortedConstraintExpr.get( thisVarIndex).varName);
                }else {
                    newJob.oneFixedVariables.add( reducedConstraint.sortedConstraintExpr.get( thisVarIndex).varName);
                }
            }
                    
            newJobs.add(newJob );
        }
        
        return newJobs ;
    }
     
    private Rectangle createFeasibleRectangle (Rectangle job,                                                
                                               List<Integer> indexOfVarsWhichCanBeFree ,
                                               UpperBoundConstraint reducedConstraint  ) {
        //create a branch using vars which are not free, and fix their values to their value at origin
        //origin is lp vertex
        Rectangle result = new Rectangle (job.zeroFixedVariables, job.oneFixedVariables ) ;
       
        for (Integer index =ZERO; index <job.isVariableFixedAtZeroAtBestVertex.size() ; index ++) {
            
            if (indexOfVarsWhichCanBeFree.contains(index)) continue;
            
            //get var value at origin, and fix it at that value
            if (job.isVariableFixedAtZeroAtBestVertex.get(index)){
                result.zeroFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(index).varName);
            }else {
                result.oneFixedVariables.add(  reducedConstraint.sortedConstraintExpr.get(index).varName);
            }
        }
        
       
         
        return result ;
    }
        

        
    private void addLeafToFeasibleCollection (Rectangle feasibleRect) throws IloException{
        if ( ! feasibleRect.findBestVertex (ubc) ) {
            System.err.println("Could not find best vertex for what is supposed to be a feasible rect, error!") ;
            exit (ONE) ;
        }
        List<Rectangle> rects= collectedFeasibleRectangles.get( feasibleRect.bestVertexValue);
        if (rects==null) rects =new ArrayList<Rectangle> ();
        rects.add(feasibleRect) ;
        collectedFeasibleRectangles.put( feasibleRect.bestVertexValue, rects);
        
    }
    
    private void addPendingJobs (List<Rectangle> jobs) throws IloException {
        for (Rectangle job : jobs){
            addPendingJob(job);
        }
    }
    
    private void addPendingJob (Rectangle job) throws IloException {
        if ( job.findBestVertex(ubc)){
            List<Rectangle> rects= this.pendingJobs.get(job.bestVertexValue);
            if (rects==null) rects =new ArrayList<Rectangle> ();
            rects.add(job) ;
            this.pendingJobs.put( job.bestVertexValue, rects);
        };
        
        
    }
    
     
 
}

 /*
                //System.out.println("tuple "+ tuple.varName);
                //if var has +ve coeff in constr and has 0 value at vertex, delta will increase by flipping it
                //if var has -ve coeff in constr and has 1 value at vertex, delta will increase by flipping it
                //in the other two cases, var is already at its "max" value , so it will not be flipped
               // boolean checkFlip = tuple.coeff >ZERO && job.isVariableFixedAtZeroAtBestVertex.get(index);
               // checkFlip = checkFlip || (tuple.coeff <ZERO && ! job.isVariableFixedAtZeroAtBestVertex.get(index));
                
                //if cant be flipped, this var is not free to be flipped
              //  if (!checkFlip) continue;
*/
