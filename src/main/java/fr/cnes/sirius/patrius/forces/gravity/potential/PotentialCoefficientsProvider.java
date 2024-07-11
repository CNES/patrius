/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.forces.gravity.potential;

import java.io.Serializable;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface used to provide gravity field coefficients.
 * 
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 */
public interface PotentialCoefficientsProvider extends Serializable {

    /**
     * Get the zonal coefficients.
     * 
     * @param normalized
     *        (true) or un-normalized (false)
     * @param n
     *        the maximal degree requested
     * @return J the zonal coefficients array.
     * @exception PatriusException
     *            if the requested maximal degree exceeds the
     *            available degree
     */
    double[] getJ(boolean normalized, int n) throws PatriusException;

    /**
     * Get the tesseral-sectorial and zonal coefficients.
     * 
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param normalized
     *        (true) or un-normalized (false)
     * @return the cosines coefficients matrix
     * @exception PatriusException
     *            if the requested maximal degree or order exceeds the
     *            available degree or order
     */
    double[][] getC(int n, int m, boolean normalized) throws PatriusException;

    /**
     * Get tesseral-sectorial coefficients.
     * 
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param normalized
     *        (true) or un-normalized (false)
     * @return the sines coefficients matrix
     * @exception PatriusException
     *            if the requested maximal degree or order exceeds the
     *            available degree or order
     */
    double[][] getS(int n, int m, boolean normalized) throws PatriusException;

    /**
     * Get the central body attraction coefficient.
     * 
     * @return mu (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getMu();

    /**
     * Get the value of the central body reference radius.
     * 
     * @return ae (m)
     */
    double getAe();

}
