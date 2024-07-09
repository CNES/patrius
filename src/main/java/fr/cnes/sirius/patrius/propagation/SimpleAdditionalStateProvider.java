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
 * @history 25/09/2015
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:25/09/2015: Creation.
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexClosedOpen;
import fr.cnes.sirius.patrius.math.utils.BinarySearchIndexOpenClosed;
import fr.cnes.sirius.patrius.math.utils.ISearchIndex;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * <p>
 * This class is a simple implementation of additionalStateProvider. It is composed of a name, a dates table, and an
 * additional states table associated to the dates. An ISearch index provides a way to find the nearest index table for
 * a given date. It provides an additional state at a date through linear interpolation of the given additional states
 * table.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 3.1
 * 
 */
public final class SimpleAdditionalStateProvider implements AdditionalStateProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 652787286651706474L;

    /** Name describing the table of double additional state parameter */
    private final String additionalStateName;

    /** Table of additional states associated to the dates table */
    private final double[][] additionalStates;

    /** Dates table */
    private final AbsoluteDate[] tDate;

    /** Class to find for a given date the nearest date index in dates table */
    private final ISearchIndex searchIndex;

    /**
     * Creates an instance of SimpleAdditionalStateProvider from a name describing
     * the additional state double table, a table of dates, and a table of additional states
     * associated to these dates.
     * 
     * @param name
     *        describes the additional state double table
     * @param dateTab
     *        table of dates
     * @param additionalStatesTab
     *        additional states associated to these dates
     * @param algo
     *        class to find the nearest date index from a given date in the date table
     *        (If null, algo will be, by default, a {@link BinarySearchIndexOpenClosed} based on a table of duration
     *        since the first date of the dates table)
     * 
     */
    public SimpleAdditionalStateProvider(final String name, final AbsoluteDate[] dateTab,
        final double[][] additionalStatesTab, final ISearchIndex algo) {
        final int length = dateTab.length;
        this.additionalStateName = name;
        this.additionalStates = additionalStatesTab;
        this.tDate = dateTab;

        if (algo == null) {
            final double[] tabIndex = new double[length];
            for (int i = 0; i < length; i++) {
                tabIndex[i] = this.tDate[i].durationFrom(this.tDate[0]);
            }
            this.searchIndex = new BinarySearchIndexClosedOpen(tabIndex);
        } else {
            this.searchIndex = algo;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.additionalStateName;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final AbsoluteDate date) {

        final double durationFromRef = date.durationFrom(this.tDate[0]);
        // Search the index
        int index0 = this.searchIndex.getIndex(durationFromRef);
        final int index1;
        if (index0 == (this.tDate.length - 1)) {
            index0 = this.tDate.length - 2;
            index1 = this.tDate.length - 1;
        } else {
            index1 = index0 + 1;
        }
        // Absolute date for two additional states to interpolate
        final AbsoluteDate t0 = this.tDate[index0];
        final double[] addState0 = this.additionalStates[index0];
        final AbsoluteDate t1 = this.tDate[index1];
        final double[] addState1 = this.additionalStates[index1];

        // Get duration between input date and the nearest date in the table
        final double durationFromT0 = date.durationFrom(t0);
        final int length = addState0.length;
        // Interpolation for each additional states component
        final double[] addStateInterpolated = new double[length];
        for (int i = 0; i < length; i++) {
            addStateInterpolated[i] = addState0[i] +
                MathLib.divide((addState1[i] - addState0[i]) * durationFromT0, t1.durationFrom(t0));
        }
        return addStateInterpolated;
    }
}
