/**
 *
 * Copyright 2011-2017 CNES
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
 * @history created 03/09/2012
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:273:20/10/2014:Minor code problems
 * VERSION::FA:514:02/02/2016:Correcting link budget computation
 * VERSION::FA:567:04/04/2016:Link budget correction
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::FA:652:28/09/2016:Link budget correction finalisation
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:708:13/12/2016: add documentation corrections
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.RFAntennaProperty;
import fr.cnes.sirius.patrius.events.sensor.RFVisibilityDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.groundstation.RFStationAntenna;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class contains the algorithm to compute the link budget knowing
 * the satellite
 * transmitter and ground receiver parameters.<br>
 * The link budget is the accounting of all of the gains and losses from the
 * transmitter
 * (the satellite), through the medium to the receiver (the ground station).
 * 
 * @concurrency immutable
 * 
 * @see RFVisibilityDetector
 * @see RFAntennaProperty
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RFLinkBudgetModel implements Serializable {

    /**
     * Boltzmann constant [dBW / Hz / K].
     */
    public static final double KDB = -228.6;
    
    /** Serial UID. */
    private static final long serialVersionUID = 8646341412516464591L;

    /**
     * The ground station antenna.
     */
    private final RFStationAntenna receiver;

    /**
     * The satellite.
     */
    private final Assembly satAssembly;

    /**
     * The satellite antenna.
     */
    private final IPart transmitter;

    /**
     * The satellite antenna RF properties.
     */
    private final RFAntennaProperty propertyRF;

    /**
     * Constructor for the link budget model.
     * 
     * @param groundAntenna
     *        the ground station antenna
     * @param satellite
     *        the satellite assembly
     * @param satelliteAntennaPart
     *        the name of the satellite antenna part in the assembly
     */
    public RFLinkBudgetModel(final RFStationAntenna groundAntenna, final Assembly satellite,
        final String satelliteAntennaPart) {
        this.receiver = groundAntenna;
        this.satAssembly = satellite;
        this.transmitter = satellite.getPart(satelliteAntennaPart);
        final boolean hasProperty = this.transmitter.hasProperty(PropertyType.RF);
        if (hasProperty) {
            this.propertyRF = (RFAntennaProperty) this.transmitter.getProperty(PropertyType.RF);
        } else {
            // no RF property is associated to the satellite transmitter!
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_RF_PROPERTY);
        }
    }

    /**
     * Computes the link budget at a given date.
     * <b> Warning : </b> in the terms used for link budget computation, the elliptic losses and the elliptic factor
     * are converted in dB using 20log(...) and not 10log(...).
     * 
     * @param date
     *        the spacecraft date of the link budget computation.
     * @return the link budget<br>
     * @throws PatriusException
     *         when computing the ground receiver coordinates.
     */
    public final double computeLinkBudget(final AbsoluteDate date) throws PatriusException {
        return computeLinkBudget(date, date);
    }

    /**
     * Computes the link budget at a given date.
     * Emitter and receiver date can be different if propagation time is taken into account
     * <b> Warning : </b> in the terms used for link budget computation, the elliptic losses and the elliptic factor
     * are converted in dB using 20log(...) and not 10log(...).
     * 
     * @param emitterDate
     *        the spacecraft emission date of the link budget computation.
     * @param receiverDate
     *        the ground antenna reception date of the link budget computation.
     * @return the link budget<br>
     * @throws PatriusException
     *         when computing the ground receiver coordinates.
     */
    public final double computeLinkBudget(final AbsoluteDate emitterDate,
            final AbsoluteDate receiverDate) throws PatriusException {

        // ============== Elevation and azimuth of station from satellite ==============

        // Antenna frame (updated with transmitter date)
        final Frame antennaFrame = this.transmitter.getFrame();

        // Ground antenna coordinates in satellite frame
        final PVCoordinates pv = this.receiver.getPVCoordinates(receiverDate, antennaFrame);

        // Computes the ground receiver elevation in the satellite transmitter frame
        final double elevation = pv.getPosition().getDelta();
        if (elevation < 0) {
            // Antenna not directed toward station
            return Double.NEGATIVE_INFINITY;
        }

        // Computes the satellite/ground receiver distance
        final double distance = pv.getPosition().getNorm();

        // Computes the polar angle (theta)
        final double polarAngle = FastMath.PI / 2.0 - elevation;
        // Computes the azimuth angle (phi)
        double azimuth = pv.getPosition().getAlpha();
        if (azimuth < 0) {
            azimuth = 2.0 * FastMath.PI + azimuth;
        }

        // ============== Elevation (site) of satellite from station ==============

        // Station frame
        final Frame groundFrame = this.receiver.getFrame();

        // Compute the transformation from antennaFrame to groundFrame at given date
        final Transform t = antennaFrame.getTransformTo(groundFrame, emitterDate);

        // The origin of antennaFrame is expressed in groundFrame :
        // it gives the transmitter position in groundFrame
        final Vector3D transmitterPos = t.transformPosition(Vector3D.ZERO);

        // Finally, compute the transmitter elevation in the receiver frame (site)
        final double transmitterElevation = transmitterPos.getDelta();

        // ============== Compute link budget ==============

        // Computes the propagation loss
        final double elliptSatdB = this.propertyRF.getEllipticity(polarAngle, azimuth);
        final double elliptSat = MathLib.pow(10., elliptSatdB / (10. * 2.));
        final double elliptGrdB = this.receiver.getEllipticityFactor();
        final double elliptGr = MathLib.pow(10., elliptGrdB / (10. * 2.));
        final double l1 = ((elliptSat + elliptGr) * (elliptSat + elliptGr)) /
            ((1. + elliptSat * elliptSat) * (1. + elliptGr * elliptGr));
        final double l1dB = -10. * MathLib.log10(l1);
        final double l = l1dB + this.receiver.getAtmosphericLoss(transmitterElevation) +
            this.receiver.getPointingLoss(transmitterElevation);
        // Computes the free-space path loss
        final double lfs = 4. * FastMath.PI * distance * this.propertyRF.getFrequency() / Constants.SPEED_OF_LIGHT;
        final double lfsdB = 10. * MathLib.log10(lfs * lfs);
        // Compute the bit rate in decibels
        final double bitRatedB = 10. * MathLib.log10(this.propertyRF.getBitRate());
        // Get the gain
        final double gaindB = this.propertyRF.getGain(polarAngle, azimuth);

        // Computes the total link budget
        final double pire = this.propertyRF.getOutputPower() + gaindB - this.propertyRF.getCircuitLoss();
        return pire +
            this.receiver.getMeritFactor() - KDB - lfsdB - bitRatedB - this.receiver.getGroundLoss()
            - this.propertyRF.getTechnoLoss() - l - this.receiver.getCombinerLoss();
    }

    /**
     * @return the assembly representing the satellite
     */
    public final Assembly getSatellite() {
        return this.satAssembly;
    }
    
    /**
     * Returns the receiver (ground antenna).
     * @return the receiver
     */
    public RFStationAntenna getReceiver() {
        return receiver;
    }
}
