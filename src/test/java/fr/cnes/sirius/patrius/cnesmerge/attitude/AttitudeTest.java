/**
 * 
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.attitude;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980HistoryLoader;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              attitude tests
 *              </p>
 * 
 * @author ClaudeD
 * 
 * @version $Id: AttitudeTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AttitudeTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Attitude
         * 
         * @featureDescription Attitude definition
         * 
         * @coveredRequirements DV-ATT_10, DV-ATT_20, DV-ATT_30, DV-ATT_40, DV-ATT_50, DV-ATT_60
         */
        ATTITUDE_CONSTRUCTOR,
        /**
         * @featureTitle Derivation
         * 
         * @featureDescription the derivation of the rotation axis
         * 
         * @coveredRequirements DV-ATT_10, DV-ATT_20, DV-ATT_30, DV-ATT_40, DV-ATT_50, DV-ATT_60
         */
        ATTITUDE_DEVIATION,
        /**
         * @featureTitle State vector
         * 
         * @featureDescription the conversion from Attitude to state array
         * 
         * @coveredRequirements
         */
        ATTITUDE_TO_ARRAY
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_CONSTRUCTOR}
     * 
     * @testedMethod {@link Attitude#Attitude(AbsoluteDate, Frame, Rotation, Vector3D)}
     * 
     * @description constructor test
     * 
     * @input none
     * 
     * @output a class instance
     * 
     * @testPassCriteria class instance not null
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     */
    @Test
    public void testAttitudeConstructor() throws PatriusException {
        final Rotation rot = new Rotation(false, 0.48, 0.64, 0.36, 0.48);
        final Attitude attitude0 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), rot,
            Vector3D.ZERO);
        Assert.assertEquals(FramesFactory.getEME2000(), attitude0.getReferenceFrame());
        Assert.assertEquals(FramesFactory.getEME2000(), attitude0.withReferenceFrame(FramesFactory.getEME2000())
            .getReferenceFrame());

        final double dt = 10.0;
        Attitude shifted;
        shifted = attitude0.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getSpin());
        Assert.assertEquals(attitude0.getRotation(), shifted.getRotation());
        Assert.assertEquals(FramesFactory.getEME2000(), shifted.getReferenceFrame());
        Attitude att;
        att = shifted.withReferenceFrame(FramesFactory.getVeis1950());
        Assert.assertEquals(FramesFactory.getVeis1950(), att.getReferenceFrame());

        final Attitude attitude1 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), rot,
            Vector3D.ZERO);
        Assert.assertEquals(FramesFactory.getEME2000(), attitude1.getReferenceFrame());

        shifted = attitude1.shiftedBy(dt);
        Assert.assertEquals(attitude1.getRotation(), shifted.getRotation());
        Assert.assertEquals(FramesFactory.getEME2000(), shifted.getReferenceFrame());
        att = shifted.withReferenceFrame(FramesFactory.getVeis1950());
        Assert.assertEquals(FramesFactory.getVeis1950(), att.getReferenceFrame());

        final Attitude attitude2 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), rot,
            Vector3D.ZERO);
        shifted = attitude2.shiftedBy(dt);
        Assert.assertEquals(FramesFactory.getEME2000(), shifted.getReferenceFrame());
        att = shifted.withReferenceFrame(FramesFactory.getVeis1950());
        Assert.assertEquals(FramesFactory.getVeis1950(), att.getReferenceFrame());

        final Attitude attitude3 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), rot,
            Vector3D.ZERO);
        shifted = attitude3.shiftedBy(dt);
        Assert.assertEquals(FramesFactory.getEME2000(), shifted.getReferenceFrame());
        att = shifted.withReferenceFrame(FramesFactory.getVeis1950());
        Assert.assertEquals(FramesFactory.getVeis1950(), att.getReferenceFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_DEVIATION}
     * 
     * @testedMethod {@link Attitude#getDate()}
     * 
     * @description getter tests
     * 
     * @input none
     * 
     * @output angular acceleration, angular jerk and angular snap
     * 
     * @testPassCriteria correctly values
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     */
    @Test
    public void testAttitudeDeviation() throws PatriusException {
        final Attitude attitude1 = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(
            false, 0.48, 0.64, 0.36, 0.48), Vector3D.ZERO);
        final double dt = 10.0;
        final Attitude shifted = attitude1.shiftedBy(dt);
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH.shiftedBy(10), shifted.getDate());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_TO_ARRAY}
     * 
     * @testedMethod {@link Attitude#mapAttitudeToArray()}
     * 
     * @description conversion to array test
     * 
     * @input none
     * 
     * @output an array
     * 
     * @testPassCriteria correctly values
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     */
    @Test
    public void testAttitudeToArray() throws PatriusException {
        final double q0 = 0.48;
        final double q1 = 0.64;
        final double q2 = 0.36;
        final double q3 = 0.48;
        final double x = 1;
        final double y = 2;
        final double z = 3;
        final Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(), new Rotation(
            false, q0, q1, q2, q3), new Vector3D(x, y, z));
        final double[] array = attitude.mapAttitudeToArray();
        final double expectedArray[] = new double[] { q0, q1, q2, q3, x, y, z };
        Assert.assertArrayEquals(expectedArray, array, 0);
    }

    /**
     * @throws PatriusException
     * @Before
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final int mjd = (int) (AbsoluteDate.J2000_EPOCH.offsetFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
            TimeScalesFactory.getTAI()) / 86400);
        EOPHistoryFactory.clearEOP1980HistoryLoaders();
        EOPHistoryFactory.addEOP1980HistoryLoader(new EOP1980HistoryLoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) throws IOException,
                                                                            ParseException, PatriusException {
                throw new IOException();

            }

            @Override
            public void fillHistory(final EOP1980History history) throws PatriusException {

                history.addEntry(new EOP1980Entry(mjd - 3, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd - 2, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd - 1, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 1, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 2, 0, 0, 0, 0, 0, 0));
                history.addEntry(new EOP1980Entry(mjd + 3, 0, 0, 0, 0, 0, 0));
            }

        });
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
