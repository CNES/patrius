/**
 * Copyright 2011-2022 CNES
 *
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
 *
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.MODPrecessionModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.CIPCoordinates;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface providing the basic services for frame configurations.
 *
 * @author Julie Anton, Gérald Mercadier
 *
 * @version $Id: FramesConfiguration.java 18073 2017-10-02 16:48:07Z bignon $
 *
 * @since 1.2
 */
public interface FramesConfiguration extends Serializable {

    /**
     * Compute corrected polar motion.
     *
     * @param date
     *        date for which one the polar motion is computed
     * @throws PatriusException
     *         when an Orekit error occurs
     * @return u, v
     */
    PoleCorrection getPolarMotion(final AbsoluteDate date) throws PatriusException;

    /**
     * Compute S' value.
     *
     * @param date
     *        date for which one S prime is computed
     * @return s'
     */
    double getSprime(final AbsoluteDate date);

    /**
     * Compute corrected ut1-tai.
     *
     * @param date
     *        date for which one the ut1-tai is computed.
     * @return ut1-tai
     */
    double getUT1MinusTAI(final AbsoluteDate date);

    /**
     * Compute correction dut1.
     *
     * @param date
     *        date for which the correction is computed.
     * @return dut1
     */
    double getUT1Correction(final AbsoluteDate date);

    /**
     * Compute the corrected Celestial Intermediate Pole motion (X, Y, S, Xdot, Ydot, Sdot) in the GCRS.
     *
     * @param date
     *        date for which one the CIP motion is computed.
     * @return X, Y, S, Xdot, Ydot, Sdot
     */
    CIPCoordinates getCIPCoordinates(final AbsoluteDate date);

    /**
     * Getter for the Earth obliquity at provided date used in MOD to Ecliptic MOD transformation.
     *
     * @param date date
     * @return the Earth obliquity at provided date used in MOD to Ecliptic MOD transformation
     */
    double getEarthObliquity(AbsoluteDate date);

    /**
     * Getter for the MOD precession transformation from GCRF/EME2000 to MOD at provided date.
     *
     * @param date date
     * @return the MOD precession rotation from GCRF/EME2000 to MOD at provided date
     */
    Rotation getMODPrecession(AbsoluteDate date);

    /**
     * Return the EOP interpolation method.
     *
     * @return eop interpolation method
     */
    EOPInterpolators getEOPInterpolationMethod();

    /**
     * Time interval of validity for the EOP files.
     *
     * @return time interval of validity as a {@link AbsoluteDateInterval}
     */
    AbsoluteDateInterval getTimeIntervalOfValidity();

    /**
     * Get the EOP history.
     *
     * @return the EOP history
     */
    EOPHistory getEOPHistory();

    /**
     * Get the polar motion model.
     *
     * @return the pola motion model
     */
    PolarMotion getPolarMotionModel();

    /**
     * Get the diurnal rotation model.
     *
     * @return the diurnal rotation model
     */
    DiurnalRotation getDiurnalRotationModel();

    /**
     * Get the CIRF precession nutation model.
     *
     * @return the CIRF precession nutation model
     */
    PrecessionNutation getCIRFPrecessionNutationModel();

    /**
     * Get the MOD precession model.
     *
     * @return the MOD precession model
     */
    MODPrecessionModel getMODPrecessionModel();
}
