/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.drivers;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.HALT_FILE;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.LOG_FOLDER;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.*;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.MIP_FILENAME;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.collection.RectangleCollector;
import ca.mcmaster.infeasiblehypercubes_3a.common.LowerBoundConstraint;
import ca.mcmaster.infeasiblehypercubes_3a.common.Objective;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import ca.mcmaster.infeasiblehypercubes_3a.utils.MIP_Reader;
import ca.mcmaster.infeasiblehypercubes_3a.utils.RectangleFilter;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public abstract class Base_Driver {
        
    //constraints in this mip
    public static List<LowerBoundConstraint> mipConstraintList ;
    public static Objective objective ;
    public static List<String> allVariablesInModel ;
    
    //here is the frontier and incumbent
    public static  Map<Double, List<LeafNode> > activeLeafs  = new TreeMap<Double,  List<LeafNode> >();    
    public static double incumbent = Double.MAX_VALUE;
    public static LeafNode bestKnownSolution = null;
    
    //1 collector per constraint
    protected List<RectangleCollector> rectangleCollectorList = new ArrayList <RectangleCollector>();
    
    protected static Logger logger=Logger.getLogger(Base_Driver.class);
        
    public   Base_Driver () throws IloException{
                
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting " + LOG_FOLDER);
            //exit(ONE);
        }
            
        logger=Logger.getLogger(Base_Driver.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+Base_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        logger.debug ("Start  solve " +MIP_FILENAME ) ;
        
        //assemble constraints in model
        IloCplex mip =  new IloCplex();
        mip.importModel(MIP_FILENAME);
        mip.exportModel(MIP_FILENAME+ ".lp");
        mipConstraintList= MIP_Reader.getConstraints(mip);
        objective= MIP_Reader.getObjective(mip);
        allVariablesInModel = MIP_Reader.getVariables(mip) ;
        
        logger.debug ("Collected objective and constraints" ) ;
        
       //prepare 1 rectangle collector per constraint        
        for ( LowerBoundConstraint lbc: mipConstraintList) {
           RectangleCollector collector=new RectangleCollector(lbc) ;
           rectangleCollectorList.add(collector) ;
           //collector.printAllPendingJobs();
           collector.collectOne();
           //collector.printAllCollectedRects();
            
           
           
        }
        
        
       
        
        
    } //end main
    
    public abstract void solve () ;
        
    public static double getBestBound () {
        return Collections.min(activeLeafs.keySet()) ;
    }
        
    protected static boolean isHaltFilePresent (){
        File file = new File( HALT_FILE);
         
        return file.exists();
    }
        
    protected static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }
     
    protected static int getNumberOfLeafs ( Map<Double, List<LeafNode> > map2){
        int count = ZERO;
        for (List<LeafNode> rectlist : map2.values()){
            count +=rectlist.size();
        }
        return count;
    }
    
    protected void printAllLeafs () {
        logger.debug("");
        for (List<LeafNode> nodes : this.activeLeafs.values()) {
            for (LeafNode node: nodes){
                logger.debug(node + " having lp relax "+ node.lpRelaxValueMinimization);
            }
            
        }
        logger.debug("");
        
    }
        
    protected void addActiveLeaf( LeafNode new_Node) {
        List<LeafNode>  nodeList = this.activeLeafs.get( new_Node.getLpRelaxVertex_Minimization());
        if (nodeList==null) nodeList = new ArrayList<LeafNode> ();
        nodeList.add(new_Node);
        this.activeLeafs.put( new_Node.lpRelaxValueMinimization , nodeList);
    }
        
    protected int replenishRectanglesAll (double threshold) throws IloException {
        int count = ZERO;
        for (RectangleCollector collector : this.rectangleCollectorList) {
            count += collector.replenishRectangles(threshold);
        }
        return count;
    }
    
    //get the   compatible rect list for each constraint, given a leaf
    //only include rects that have better LP than leaf
    //return true if infeasible
    protected boolean getCompatibleRectanglesFromEveryConstraint (LeafNode leaf,  Map<Double, List<Rectangle>  > compatibleRectMap) {
        
        boolean isInfeasible   =false;
        for ( RectangleCollector collector: rectangleCollectorList ) {
             for (double lp: collector.collectedFeasibleRectangles.keySet()) {
                 
                 if (lp > leaf.lpRelaxValueMinimization)  break;
                 
                 //get all compatible rects at this lp
                 List<Rectangle>  compatible = new ArrayList<Rectangle>  ();
                 RectangleFilter filter = new RectangleFilter () ;
                 isInfeasible   = filter.getCompatibleRectangles ( collector.collectedFeasibleRectangles.get(lp), leaf ,compatible) ;
                   
                 
                 if ( isInfeasible   ) {
                     break;
                 } else if (compatible.size()>ZERO) {
                     //leaf still is not found unfeasible and some compatible rects found
                     List<Rectangle>  current = compatibleRectMap.get(lp);
                     if (current==null) current = new ArrayList<Rectangle> ();
                     current.addAll (compatible) ;
                     compatibleRectMap.put (lp, current) ;
                 }
             }
             if ( isInfeasible   ) break;
        }
        return  isInfeasible    ;
    }
    
    protected void createChildNodes (LeafNode leaf, String branchingVar) {
        LeafNode zeroChild = new LeafNode (new ArrayList<String> (), new ArrayList<String> ()) ;
        zeroChild.zeroFixedVariables.addAll( leaf.zeroFixedVariables);
        zeroChild.oneFixedVariables.addAll( leaf.oneFixedVariables);
        zeroChild.zeroFixedVariables.add( branchingVar);
        
        LeafNode oneChild = new LeafNode (new ArrayList<String> (), new ArrayList<String> ()) ;
        oneChild.zeroFixedVariables.addAll( leaf.zeroFixedVariables);
        oneChild.oneFixedVariables.addAll( leaf.oneFixedVariables);
        oneChild.oneFixedVariables.add( branchingVar);
        
        this.addActiveLeaf(zeroChild);
        this.addActiveLeaf(oneChild);
        
    }
}
