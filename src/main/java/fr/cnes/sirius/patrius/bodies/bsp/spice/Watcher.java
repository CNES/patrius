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

import java.util.LinkedList;
import java.util.List;

/**
 * This class make up the data structure that maps variables to
 * their associated agents. This will allow to notify all the agents
 * if there is a change in the variable.
 * 
 * This class is based on a data structure described at POOL.for
 * from the SPICE library.
 * @author T0281925
 *
 */
public class Watcher implements Comparable<Watcher> {

    /**
     * Variable name
     */
    private final String varName;
    /**
     * Agents (methods/class) associated to the variable
     */
    private final List<String> agents;

    /**
     * Constructor
     * @param var variable to which we want to set a watcher
     * @throws IllegalArgumentException if var is null or empty String
     */
    public Watcher(final String var) throws IllegalArgumentException {
        if (var == null) {
            throw new IllegalArgumentException();
        } else if (var.isEmpty() ) {
            throw new IllegalArgumentException();
        }
        varName = var;
        agents = new LinkedList<String>();
    }

    /**
     * Get the variable name of the watcher
     * @return the variable name of the watcher
     */
    public String getVarName() {
        return varName;
    }

    /**
     * Get the list of agents to be notified if the variable change
     * @return the list of agents associated to the variable
     */
    public List<String> getAgents() {
        return agents;
    }

    /**
     * Add an agent to the list
     * @param agent agent to be added to the list
     */
    public void addAgent(final String agent) {
        agents.add(agent);
    }

    /**
     * Compare 2 watchers to be used than a Set
     */
    @Override
    public int compareTo(final Watcher w) {
        return varName.compareTo(w.varName);
    }

    /**
     * Calculate the hash code for a Watcher
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + varName.hashCode();
        return result;
    }

    /**
     * Determine if 2 watchers are looking at the same variable
     */
    @Override
    public boolean equals(final Object obj) {
        // Check the object can be of the type Watcher
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }            
        if (getClass() != obj.getClass()) {
            return false;
        }           
        // Instantiate a watcher
        final Watcher other = (Watcher) obj;
        // Compare the variable name
        return varName.equals(other.varName);
    }

}
