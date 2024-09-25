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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * This class make up the data structure that maps variables to their associated agents. This will allow to notify all
 * the agents if there is a change in the variable.
 * <p>
 * This class is based on a data structure described at POOL.for from the SPICE library.
 * </p>
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public class Watcher implements Comparable<Watcher> {

    /** Variable name. */
    private final String varName;

    /** Agents (methods/class) associated to the variable. */
    private final List<String> agents;

    /**
     * Constructor.
     * 
     * @param var
     *        variable to which we want to set a watcher
     * @throws IllegalArgumentException
     *         if var is null or empty String
     */
    public Watcher(final String var) throws IllegalArgumentException {
        if (var == null || var.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.varName = var;
        this.agents = new LinkedList<>();
    }

    /**
     * Getter for the variable name of the watcher.
     * 
     * @return the variable name of the watcher
     */
    public String getVarName() {
        return this.varName;
    }

    /**
     * Getter for the list of agents to be notified if the variable change.
     * 
     * @return the list of agents associated to the variable
     */
    public List<String> getAgents() {
        return this.agents;
    }

    /**
     * Add an agent to the list.
     * 
     * @param agent
     *        agent to be added to the list
     */
    public void addAgent(final String agent) {
        this.agents.add(agent);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final Watcher w) {
        return this.varName.compareTo(w.varName);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.varName.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        boolean isEqual = false;

        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            final Watcher other = (Watcher) obj;
            isEqual = Objects.equals(this.varName, other.varName);
        }

        return isEqual;
    }
}
