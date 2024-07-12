/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class class reproduces the necessary routines of the pool.for file in original Spice library.
 * <p>
 *
 * @author
 *
 * @since 4.11
 */
public final class PoolSpice {
    /**
     * is the maximum number of variables that the
     * kernel pool may contain at any one time.
     * MAXVAR should be a prime number.
     */
    private static final  int MAXVAR = 26003;
    /**
     * is the maximum sum of the sizes of the sets of
     * agents in the range of the mapping that associates
     * with each watched kernel variable a set of agents
     * that "watch" that variable.
     */
    private static final int MAXNOTE = MAXVAR * 5;

    /**
     * List holding kernel pool variable. 
     * This is an adaptation of a bunch of variables 
     * declared in Pool.for (SPICE library) including
     * NAMLST,NMPOOL,PNAME,DATLST,DPPOOL,DPVALS,CHPOOL,CHVALS
     */
    private static List<KernelPool> kernelPool;
    /**
     * Numerical kernel pool type
     */
    private static final String NUM = "numerical";
    /**
     * Character kernel pool type
     */
    private static final String STR = "character";

    /** 
     * Set for mapping each variable to the list of agents associated
     */
    private static Set<Watcher> watchedVariables;

    /**
     * Agents contains the list of agents that need to be notified
     * about updates to their variables.
     */
    private static List<String> agents;
    /**
     * Temporary set
     */
    private static List<String> active;

    /**
     * Boolean indicating whether it is the first time calling a method
     */
    private static boolean first = true;

    /**
     * POOL state counter.
     */
    private static CounterArray subctr;

    /**
     * Constructor
     */
    private PoolSpice() {
        // Nothing to do
    }

    /**
     * This routine initializes the data structures needed for
     * maintaining the kernel pool
     * @throws PatriusException 
     */
    public static void init() throws PatriusException {
        watchedVariables = new TreeSet<Watcher>();
        kernelPool = new ArrayList<KernelPool>();

        active = new ArrayList<String>(MAXNOTE);
        agents = new ArrayList<String>(MAXNOTE);

        subctr = new CounterArray("subsystem");
    }

    /**
     * clPool clears the pool of kernel variables maintained by
     * the kernel POOL subsystem. All the variables in the pool are
     * deleted. However, all watcher information is retained.
     * 
     * Each watched variable will be regarded as having been updated.
     * Any agent associated with that variable will have a notice
     * posted for it indicating that its watched variable has been
     * updated.
     * <p>
     * Inspired by the CLPOOL routine in pool.for
     * @throws PatriusException 
     */
    public static void clpool() throws PatriusException {
        // Initialize the pool if necessary
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }

        // Increment POOL state counter
        subctr.increment();

        // Wipe out all kernelPool
        kernelPool.clear();
    }

    /**
     * Add a name to the list of agents to notify whenever a member of
     * a list of kernel variables is updated.
     * Inspired by SWPOOL in the SPICE library.
     * 
     * @param agent The name of an agent to be notified after updates.
     * @param nnames The number of variables to associate with AGENT.
     * @param names Variable names whose update causes the notice.
     * @throws PatriusException if there is a problem incrementing the counter or the number of agents is too large
     */
    public static void setWatch(final String agent,
            final int nnames,
            final String[] names) throws PatriusException {
        // Initialize the pool if necessary
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }

        // Increment POOL state counter. Although setting a watcher does not
        // change the POOL we will increment the POOL state counter to make
        // sure that the next call to CVPOOL with this watcher triggers the
        // initial update.
        subctr.increment();

        // There is no need to check if there is enough space in the structures.

        // For each variable specified by the array NAMES, put AGENT
        // into its list of guys to be notified when a variable change
        // occurs.

        for (int i = 0; i < nnames; i++) {

            // Get the agents associated with NAMES(I).
            active = getAgentsOfVar(names[i]);

            final int nfetch = active.size();
            // Three things can happen now:
            //
            // 1) The kernel variable NAMES(I) is already watched by at
            // least one agent, but not by AGENT. We need to add AGENT
            // to the list of agents watching NAMES(I).
            //
            // 2) The kernel variable NAMES(I) isn't yet watched by any
            // agent, so we need to insert NAMES(I) into WTVARS, as
            // well as add AGENT to the (empty) list of agents watching
            // NAMES(I).
            //
            // 3) The kernel variable NAMES(I) is already watched by AGENT.
            // No action is needed.
            //
            // We could get fancy and try to minimize the number of lines of
            // code required to handle the first two cases...but we won't.
            // We'll just take them one at a time.

            if (nfetch > 0) {
                if (!active.contains(agent)) {
                    // Case 1: at least one agent is already watching NAMES(I),
                    // but AGENT is not watching NAMES(I). We need the head of
                    // the agent list for this kernel variable.
                    ((TreeSet<Watcher>) watchedVariables).floor(new Watcher(names[i])).addAgent(agent);
                }
            } else {
                // Case 2: the kernel variable NAMES(I) isn't watched. Add it
                // the watcher system. We've already ensured that there's
                // room in WTVARS and WTAGNT and that the insertion won't give
                // NAMES(I) an excessive number of agents.

                final Watcher w = new Watcher(names[i]);
                w.addAgent(agent);
                watchedVariables.add(w);
            }

        }

        // We ALWAYS put this agent into the list of agents to be notified.
        agents.add(agent);
    }

    /**
     * Indicate whether or not any watched kernel variables that have a
     * specified agent on their notification list have been updated.
     * Inspired by CVPOOL of the SPICE library.
     * 
     * @param agent Name of the agent to check for notices.
     * @return TRUE if variables for AGENT have been updated.
     * @throws PatriusException 
     */
    public static boolean checkUpdates(final String agent) throws PatriusException {
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }

        return agents.remove(agent);

    }

    /**
     * Return the character value of a kernel variable from the
     * kernel pool.
     * Inspired by GCPOOL and STPOOL from the SPICE library. Doing th job for both.
     * 
     * @param var Name of the variable whose value is to be returned.
     * @return Values associated with NAME.
     * @throws PatriusException 
     */
    public static List<String> getStrVals(final String var) throws PatriusException {
        // Initialize the pool if necessary.
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }

     // Initialize return list
        final ArrayList<String> stringList = new ArrayList<String>();
        
        // We look for a numerical kernel pool associated to VAR.
        // We create a var of the type to check if it exists in the list
        final KernelPool testVar = new KernelPool(var,STR);
        final int index = kernelPool.indexOf(testVar);
        
        if (index == -1) {
            // Either the name is not in the kernel pool, or the type 
            // is not the one desired.
            // Return an empty list as a signal of not found
            stringList.clear();
        } else {
            throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);     
        }
        
        return stringList;
    }
    
    /**
     * Return the integer value of a kernel variable from the kernel pool.
     * Inspired by GIPOOL from the SPICE library.
     * 
     * @param var name of the variable whose values are to be returned.
     * @return the array of values associated with NAME. Any
     *         numeric value having non-zero fractional part is
     *         rounded to the closest integer.
     * @throws PatriusException if there is a problem in the initialization process
     */
    public static List<Integer> getIntVals(final String var) throws PatriusException {
        // Initialize the pool if necessary.
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }
        
        // Initialize return list
        final ArrayList<Integer> integerList = new ArrayList<Integer>();
        
        // We look for a numerical kernel pool associated to VAR.
        // We create a var of the type to check if it exists in the list
        final KernelPool testVar = new KernelPool(var,NUM);
        final int index = kernelPool.indexOf(testVar);
        
        if (index == -1) {
            // Either the name is not in the kernel pool, or the type 
            // is not the one desired.
            // Return an empty list as a signal of not found
            integerList.clear();
        } else {
            throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);      
        }
        
        return integerList;
    }

    /**
     * Return a SPICE set containing the names of agents watching
     * a specified kernel variable.
     * Inspired by ZZGAPOOL from the SPICE library.
     * 
     * @param var Kernel variable name.
     * @return a list containing the names of agents watching var.
     */
    private static List<String> getAgentsOfVar(final String var) {
        // Get to know if the variable is being watched
        final boolean contains = watchedVariables.contains(new Watcher(var));

        ArrayList<String> agentsList = new ArrayList<String>();
        Watcher vars;

        if (contains) {
            // If it is watched, iterate to find it and get the list
            // of agents associated with it
            final Iterator<Watcher> it = watchedVariables.iterator();
            boolean found = false;
            while (it.hasNext() && !found) {
                vars = it.next();
                if (vars.getVarName().equals(var)) {
                    found = true;
                    agentsList = (ArrayList<String>) vars.getAgents();
                }
            }
        }
        
        return agentsList;
    }

    /**
     * Check and update the POOL state counter tracked by a caller
     * (user) routine.
     * 
     * Translation of ZZPCTRCK from the Spice library
     * 
     * @param c State counter to check
     * @return Flag indicating if input counter was updated
     * @throws PatriusException 
     */
    public static boolean checkPoolStateCounter(final CounterArray c) throws PatriusException {
        if (PoolSpice.first) {
            init();
            PoolSpice.first = false;
        }
        return c.checkAndUpdate(subctr);
    }
    
    /**
     * Determine whether or not any of the POOL variables that are to be
     * watched and have AGENT on their distribution list have been
     * updated, but do the full watcher check only if the POOL state
     * counter has changed.
     * 
     * Translation of ZZCVPOOL from the Spice Library
     * 
     * @param c POOL state counter tracked by the caller
     * @param agent name of the agent (routine) that need acess to the kernel pool
     * @return logical flag that will be set to true if the
     *           variables in the kernel pool that are required by
     *           AGENT have been updated since the last call to
     *           CVPOOL.
     * @throws PatriusException 
     */
    public static boolean checkUpdatesIfCounterUpdate(final CounterArray c, 
                                                      final String agent) throws PatriusException {
        
        // Check/update counter.
        boolean update = checkPoolStateCounter( c );
        
        // If counter was updated, check in and call checkUpdates.
        if ( update ) {
            update = checkUpdates( agent );
        }
        
        return update;       
    }
}
