/**
 * Copyright 2011-2022 CNES
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
 *
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
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

import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History;
import fr.cnes.sirius.patrius.frames.configuration.eop.NutationCorrection;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecession;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecessionConvention;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.MODPrecessionModel;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.CIPCoordinates;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutation;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a frames configuration.
 *
 * @author Julie Anton, Rami Houdroge
 * @version $Id: FramesConfigurationImplementation.java 18073 2017-10-02 16:48:07Z bignon $
 * @since 1.2
 */
public class FramesConfigurationImplementation implements FramesConfiguration {

    /** Serializable UID. */
    private static final long serialVersionUID = -7815355641864558021L;

    /** EOP history. */
    private EOPHistory historyEOP = new NoEOP2000History();

    /** Polar motion model. */
    private PolarMotion polarMotion = new PolarMotion(false, TidalCorrectionModelFactory.NO_TIDE,
        LibrationCorrectionModelFactory.NO_LIBRATION, SPrimeModelFactory.NO_SP);

    /** Diurnal rotation model. */
    private DiurnalRotation diurnalRotation = new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
        LibrationCorrectionModelFactory.NO_LIBRATION);

    /** CIRF precession nutation model. */
    private PrecessionNutation cirfPrecessionNutation = new PrecessionNutation(false,
        PrecessionNutationModelFactory.NO_PN);

    /** MOD Precession model. */
    private MODPrecessionModel modPrecession = new IAUMODPrecession(IAUMODPrecessionConvention.IAU1976, 1, 3);

    /**
     * Protected constructor.
     */
    protected FramesConfigurationImplementation() {
        // Nothing to do
    }

    /**
     * Set the polar motion model.
     *
     * @param model
     *        polar motion model
     */
    protected void setPolarMotionModel(final PolarMotion model) {
        this.polarMotion = model;
    }

    /**
     * Set the diurnal rotation model.
     *
     * @param model
     *        diurnal rotation model
     */
    protected void setDiurnalRotationModel(final DiurnalRotation model) {
        this.diurnalRotation = model;
    }

    /**
     * Set the precession nutation model.
     *
     * @param model
     *        precession nutation model
     */
    protected void setCIRFPrecessionNutationModel(final PrecessionNutation model) {
        this.cirfPrecessionNutation = model;
    }

    /**
     * Set the MOD precession model.
     *
     * @param model
     *        MOD precession
     */
    protected void setMODPrecessionModel(final MODPrecessionModel model) {
        this.modPrecession = model;
    }

    /**
     * Set the EOP history.
     *
     * @param eopHistory
     *        EOP history
     */
    protected void setEOPHistory(final EOPHistory eopHistory) {
        this.historyEOP = eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPolarMotion(final AbsoluteDate date) throws PatriusException {
        PoleCorrection pole = this.polarMotion.getPoleCorrection(date);
        if (this.polarMotion.useEopData()) {
            final PoleCorrection eopCorrection = this.historyEOP.getPoleCorrection(date);
            pole = new PoleCorrection(pole.getXp() + eopCorrection.getXp(), pole.getYp() + eopCorrection.getYp());
        }
        return pole;
    }

    /** {@inheritDoc} */
    @Override
    public double getSprime(final AbsoluteDate date) {
        return this.polarMotion.getSP(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1MinusTAI(final AbsoluteDate date) {
        return this.historyEOP.getUT1MinusTAI(date) + this.getUT1Correction(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        return this.diurnalRotation.getUT1Correction(date);
    }

    /** {@inheritDoc} */
    @Override
    public CIPCoordinates getCIPCoordinates(final AbsoluteDate date) {
        CIPCoordinates cip = this.cirfPrecessionNutation.getCIPCoordinates(date);
        if (this.cirfPrecessionNutation.useEopData()) {
            final NutationCorrection nutationCorrection = this.historyEOP.getNutationCorrection(date);
            cip = new CIPCoordinates(cip.getDate(), cip.getX() + nutationCorrection.getDX(), cip.getxP(), cip.getY()
                    + nutationCorrection.getDY(),
                cip.getyP(), cip.getS(), cip.getsP());
        }
        return cip;
    }

    /** {@inheritDoc} */
    @Override
    public double getEarthObliquity(final AbsoluteDate date) {
        return modPrecession.getEarthObliquity(date);
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getMODPrecession(final AbsoluteDate date) {
        return modPrecession.getMODPrecession(date);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeIntervalOfValidity() {
        return new AbsoluteDateInterval(IntervalEndpointType.CLOSED, this.historyEOP.getStartDate(),
            this.historyEOP.getEndDate(), IntervalEndpointType.CLOSED);
    }

    /** {@inheritDoc} */
    @Override
    public EOPInterpolators getEOPInterpolationMethod() {
        return this.historyEOP.getEOPInterpolationMethod();
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return this.historyEOP;
    }

    /** {@inheritDoc} */
    @Override
    public PolarMotion getPolarMotionModel() {
        return this.polarMotion;
    }

    /** {@inheritDoc} */
    @Override
    public DiurnalRotation getDiurnalRotationModel() {
        return this.diurnalRotation;
    }

    /** {@inheritDoc} */
    @Override
    public PrecessionNutation getCIRFPrecessionNutationModel() {
        return this.cirfPrecessionNutation;
    }

    /** {@inheritDoc} */
    @Override
    public MODPrecessionModel getMODPrecessionModel() {
        return this.modPrecession;
    }
}
