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
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:484:25/09/2015:Get additional state from an AbsoluteDate
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.AdditionalStateProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for the class StelaDifferentialEquations.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaBasicInterpolatorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela GTO differential equations system
         * 
         * @featureDescription test the Stela GTO differential equations system
         * 
         * @coveredRequirements
         */
        STELA_GTO_INTERPOLATION
    }

    /** A Stela GTO propagator. */
    private StelaBasicInterpolator interpolator;

    /** The initial date */
    private AbsoluteDate dateIn;

    /** The initial spacecraftState */
    private SpacecraftState scIn;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_INTERPOLATION}
     * 
     * @testedMethod {@link StelaDifferentialEquations#StelaDifferentialEquations(StelaGTOPropagator)}
     * @testedMethod {@link StelaDifferentialEquations#computeDerivatives(double, double[], double[])}
     * 
     * @description test the computation of the time derivative of the state vector when no forces are added
     *              to the propagator (only keplerian contribution).
     * 
     * @input a Stela GTO propagator
     * 
     * @output the time derivative of the state vector
     * 
     * @testPassCriteria the output derivatives are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testInterpolator() throws PatriusException {

        final AbsoluteDate dateInter = new AbsoluteDate(new DateComponents(2013, 4, 2), TimeScalesFactory.getTAI());
        this.interpolator.setInterpolatedDate(dateInter);

        // test getInterpolatedDate
        Assert.assertEquals(0, dateInter.durationFrom(this.interpolator.getInterpolatedDate()), 0);

        this.interpolator.setInterpolatedDate(this.dateIn);

        Assert.assertEquals(this.scIn.getA(), this.interpolator.getInterpolatedState().getA(), 0);

        try {
            this.interpolator.getInterpolatedState().getAdditionalState("unknown");
            Assert.assertFalse(true);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        // test getInitialState
        final SpacecraftState initgotten = this.interpolator.getInitialState();
        Assert.assertEquals(0, initgotten.getDate().durationFrom(this.scIn.getDate()), 0);
        Assert.assertEquals(this.scIn.getA(), initgotten.getA(), 0);
        Assert.assertEquals(this.scIn.getHx(), initgotten.getHx(), 0);

        // test 0 linear coeff
        this.interpolator.storeSC(this.scIn, this.scIn);
        this.interpolator.setInterpolatedDate(this.scIn.getDate().shiftedBy(147.));
        final SpacecraftState intergotten = this.interpolator.getInterpolatedState();
        Assert.assertEquals(0, intergotten.getDate().durationFrom(this.scIn.getDate()), 147.);
        Assert.assertEquals(this.scIn.getA(), intergotten.getA(), 0);
        Assert.assertEquals(this.scIn.getHx(), intergotten.getHx(), 0);

        // tests additionalproviders
        final AdditionalStateProvider stpro = new AdditionalStateProvider(){

            /** Serializable UID. */
            private static final long serialVersionUID = -9126595050544975103L;

            @Override
            public String getName() {

                return "hello";
            }

            @Override
            public double[] getAdditionalState(final AbsoluteDate date) throws PropagationException {
                final double[] testValues = { 2.45E7, 0.0, 0.3, 0.4, 0.4, 0.5 };
                return testValues;
            }
        };

        final List<AdditionalStateProvider> additionalStateProviders = new ArrayList<AdditionalStateProvider>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 6277329030732721761L;
        };

        additionalStateProviders.add(stpro);
        final double[] addStates = stpro.getAdditionalState(this.scIn.getDate());
        this.scIn = this.scIn.addAdditionalState("hello", addStates);
        this.interpolator.storeSC(this.scIn, this.scIn);
        this.interpolator.setAdditionalStateProviders(additionalStateProviders);
        this.interpolator.setInterpolatedDate(this.scIn.getDate());

        final double[] interAdd = this.interpolator.getInterpolatedState().getAdditionalState("hello");

        Assert.assertEquals(24500000, interAdd[0], 0);
        Assert.assertEquals(0.0, interAdd[1], 0);
        Assert.assertEquals(0.3, interAdd[2], 0);
        Assert.assertEquals(0.4, interAdd[3], 0);
        Assert.assertEquals(0.4, interAdd[4], 0);
        Assert.assertEquals(0.5, interAdd[5], 0);
    }

    /**
     * Setup method
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Before
    public void setUp() throws PatriusException, IOException, ParseException {
        this.interpolator = new StelaBasicInterpolator();

        this.dateIn = new AbsoluteDate(new DateComponents(2013, 4, 1), TimeScalesFactory.getTAI());
        final Orbit orbitIn =
            new StelaEquinoctialOrbit(24500000, 0.3, 0.4, 0.4, 0.5, 0, FramesFactory.getMOD(false), this.dateIn,
                398600441449820.0);
        final Attitude attitude = new LofOffset(orbitIn.getFrame(), LOFType.LVLH).getAttitude(orbitIn,
            orbitIn.getDate(),
            orbitIn.getFrame());
        this.scIn = new SpacecraftState(orbitIn, attitude);

        final AbsoluteDate dateOut = new AbsoluteDate(new DateComponents(2013, 4, 3), TimeScalesFactory.getTAI());
        final Orbit orbitOut =
            new StelaEquinoctialOrbit(24600000, 0.4, 0.3, 0.5, 0.4, 0.5, FramesFactory.getMOD(false),
                dateOut, 398600441449820.0);
        final SpacecraftState scOut = new SpacecraftState(orbitOut);

        this.interpolator.storeSC(this.scIn, scOut);
    }

}
