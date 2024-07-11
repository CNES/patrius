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
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.sp.SPrimeModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
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
    /** IUD. */
    private static final long serialVersionUID = -7815355641864558021L;
    /** EOP history. */
    private EOPHistory historyEOP = new NoEOP2000History();
    /** Polar motion model. */
    private PolarMotion polarMotion = new PolarMotion(false, TidalCorrectionModelFactory.NO_TIDE,
        LibrationCorrectionModelFactory.NO_LIBRATION, SPrimeModelFactory.NO_SP);
    /** Diurnal rotation model. */
    private DiurnalRotation diurnalRotation = new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
        LibrationCorrectionModelFactory.NO_LIBRATION);
    /** Precession nutation model. */
    private PrecessionNutation precessionNutation = new PrecessionNutation(false, PrecessionNutationModelFactory.NO_PN);

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
    protected void setPrecessionNutationModel(final PrecessionNutation model) {
        this.precessionNutation = model;
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
    public double[] getPolarMotion(final AbsoluteDate date) throws PatriusException {
        final PoleCorrection pole;
        if (this.polarMotion.useEopData()) {
            pole = this.historyEOP.getPoleCorrection(date);
        } else {
            pole = PoleCorrection.NULL_CORRECTION;
        }
        final PoleCorrection correction = this.polarMotion.getPoleCorrection(date);
        return new double[] { pole.getXp() + correction.getXp(), pole.getYp() + correction.getYp() };
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
    public double[] getCIPMotion(final AbsoluteDate date) {

        // get cip array
        final double[] cip = this.precessionNutation.getCIPMotion(date);
        final NutationCorrection corr;
        if (this.precessionNutation.useEopData()) {
            corr = this.historyEOP.getNutationCorrection(date);
        } else {
            // null correction if the nutation correction does not used
            corr = NutationCorrection.NULL_CORRECTION;
        }
        // initialize cip corrected array
        final double[] cipCorrected = new double[3];
        cipCorrected[0] = cip[0] + corr.getDX();
        cipCorrected[1] = cip[1] + corr.getDY();
        cipCorrected[2] = cip[2];
        return cipCorrected;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCIPMotionTimeDerivative(final AbsoluteDate date) {
        return this.precessionNutation.getCIPMotionTimeDerivative(date);
    }

    /**
     * Get the time interval of validity.
     * 
     * @return time interval of validity
     */
    @Override
    public AbsoluteDateInterval getTimeIntervalOfValidity() {
        return new AbsoluteDateInterval(IntervalEndpointType.CLOSED, this.historyEOP.getStartDate(),
            this.historyEOP.getEndDate(), IntervalEndpointType.CLOSED);
    }

    /**
     * Get the EOP interpolation method.
     * 
     * @return the EOP interpolation method used
     */
    @Override
    public EOPInterpolators getEOPInterpolationMethod() {
        return this.historyEOP.getEOPInterpolationMethod();
    }

    /**
     * Get the EOP history.
     * 
     * @return the EOP history
     */
    @Override
    public EOPHistory getEOPHistory() {
        return this.historyEOP;
    }

    /**
     * Get the polar motion model.
     * 
     * @return the pola motion model
     */
    @Override
    public PolarMotion getPolarMotionModel() {
        return this.polarMotion;
    }

    /**
     * Get the diurnal rotation model.
     * 
     * @return the diurnal rotation model
     */
    @Override
    public DiurnalRotation getDiurnalRotationModel() {
        return this.diurnalRotation;
    }

    /**
     * Get the precession nutation model.
     * 
     * @return the precession nutation model
     */
    @Override
    public PrecessionNutation getPrecessionNutationModel() {
        return this.precessionNutation;
    }
}
