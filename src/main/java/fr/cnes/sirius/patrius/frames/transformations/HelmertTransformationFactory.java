/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;

/**
 * Some {@link HelmertTransformation Helmert transformations}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: HelmertTransformationFactory.java 18074 2017-10-02 16:48:51Z bignon $
 */
public final class HelmertTransformationFactory {

    /**
     * Helmert transformation between ITRF2005 and ITRF 2008.<br>
     * see http://itrf.ign.fr/ITRF_solutions/2008/tp_08-05.php<br>
     * <br>
     * 
     * {@code T1 T2 T3 D R1 R2 R3}<br>
     * {@code mm mm mm 10-9 mas mas mas}<br>
     * {@code -0.5 -0.9 -4.7 0.94 0.000 0.000 0.000}<br>
     * {@code +/- 0.2 0.2 0.2 0.03 0.008 0.008 0.008}<br>
     * <br>
     * {@code Rates 0.3 0.0 0.0 0.00 0.000 0.000 0.000}<br>
     * {@code +/- 0.2 0.2 0.2 0.03 0.008 0.008 0.008}<br>
     * <br>
     * Table 1: Transformation parameters at epoch 2005.0 and their rates from ITRF2008 to ITRF2005
     * (ITRF2005 minus ITRF2008)<br>
     */
    public static final HelmertTransformation ITRF05TO08 = new HelmertTransformation(new AbsoluteDate(2005, 1, 1, 12,
        0, 0,
        TimeScalesFactory.getTT()), 0.5, 0.9, 4.7, 0.000, 0.000, 0.000, -0.3, 0.0, 0.0, 0.000, 0.000, 0.000);

    /**
     * Helmert transformation between ITRF2005 and ITRF 2000.<br>
     * see http://itrf.ign.fr/ITRF_solutions/2005/tp_05-00.php<br>
     * <br>
     * {@code T1 T2 T3 D R1 R2 R3}<br>
     * {@code mm mm mm 10-9 mas mas mas}<br>
     * {@code 0.1 -0.8 -5.8 0.40 0.000 0.000 0.000}<br>
     * {@code +/- 0.3 0.3 0.3 0.05 0.012 0.012 0.012}<br>
     * <br>
     * {@code Rates -0.2 0.1 -1.8 0.08 0.000 0.000 0.000}<br>
     * {@code +/- 0.3 0.3 0.3 0.05 0.012 0.012 0.012}<br>
     * <br>
     * <br>
     * Table 1: Transformation parameters at epoch 2000.0 and their rates from ITRF2005 to ITRF2000
     * (ITRF2000 minus ITRF2005)
     */
    public static final HelmertTransformation ITRF05TO00 = new HelmertTransformation(new AbsoluteDate(2000, 1, 1, 12,
        0, 0,
        TimeScalesFactory.getTT()), 0.1, -0.8, -5.8, 0.000, 0.000, 0.000, -0.2, 0.1, -1.8, 0.000, 0.000, 0.000);

    /**
     * Helmert transformation between ITRF2000 and ITRF97<br>
     * see ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP<br>
     * -------------------------------------------------------------------------------------<br>
     * SOLUTION T1 T2 T3 D R1 R2 R3 EPOCH Ref.<br>
     * UNITS----------> cm cm cm ppb .001"   .001" .001" IERS Tech.<br>
     * . . . . . . . Note #<br>
     * RATES T1 T2 T3 D R1 R2 R3<br>
     * UNITS----------> cm/y cm/y cm/y ppb/y .001"/y .001"/y .001"/y<br>
     * -------------------------------------------------------------------------------------<br>
     * ITRF97 0.67 0.61 -1.85 1.55 0.00 0.00 0.00 1997.0 27<br>
     * rates 0.00 -0.06 -0.14 0.01 0.00 0.00 0.02<br>
     * ...<br>
     * <br>
     * Note : These parameters are derived from those already published in the IERS
     * Technical Notes indicated in the table above. The transformation parameters
     * should be used with the standard model (1) given below and are valid at the
     * indicated epoch.<br>
     * <br>
     * : XS : : X : : T1 : : D -R3 R2 : : X :<br>
     * : : : : : : : : : :<br>
     * : YS : = : Y : + : T2 : + : R3 D -R1 : : Y : (1)<br>
     * : : : : : : : : : :<br>
     * : ZS : : Z : : T3 : : -R2 R1 D : : Z :<br>
     * <br>
     * Where X,Y,Z are the coordinates in ITRF2000 and XS,YS,ZS are the coordinates in
     * the other frames.
     */
    public static final HelmertTransformation ITRF00TO97 = new HelmertTransformation(new AbsoluteDate(1997, 1, 1, 12,
        0, 0,
        TimeScalesFactory.getTT()), 6.7, 6.1, -18.5, 0.00, 0.00, 0.00, 0.0, -0.6, -1.4, 0.00, 0.00, 0.002);

    /**
     * Helmert transformation between ITRF2000 and ITRF93.<br>
     * // see ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP<br>
     * // -------------------------------------------------------------------------------------<br>
     * // SOLUTION T1 T2 T3 D R1 R2 R3 EPOCH Ref.<br>
     * // UNITS----------> cm cm cm ppb .001"   .001" .001" IERS Tech.<br>
     * // . . . . . . . Note #<br>
     * // RATES T1 T2 T3 D R1 R2 R3<br>
     * // UNITS----------> cm/y cm/y cm/y ppb/y .001"/y .001"/y .001"/y<br>
     * // -------------------------------------------------------------------------------------<br>
     * // ...<br>
     * // ITRF93 1.27 0.65 -2.09 1.95 -0.39 0.80 -1.14 1988.0 18<br>
     * // rates -0.29 -0.02 -0.06 0.01 -0.11 -0.19 0.07<br>
     * // ...<br>
     * //<br>
     * // Note : These parameters are derived from those already published in the IERS
     * // Technical Notes indicated in the table above. The transformation parameters
     * // should be used with the standard model (1) given below and are valid at the
     * // indicated epoch.
     * //<br>
     * <br>
     * // : XS : : X : : T1 : : D -R3 R2 : : X :<br>
     * // : : : : : : : : : :<br>
     * // : YS : = : Y : + : T2 : + : R3 D -R1 : : Y : (1)<br>
     * // : : : : : : : : : :<br>
     * // : ZS : : Z : : T3 : : -R2 R1 D : : Z :<br>
     * //<br>
     * // Where X,Y,Z are the coordinates in ITRF2000 and XS,YS,ZS are the coordinates in
     * // the other frames.<br>
     */
    public static final HelmertTransformation ITRF00TO93 = new HelmertTransformation(new AbsoluteDate(1988, 1, 1, 12,
        0, 0,
        TimeScalesFactory.getTT()), 12.7, 6.5, -20.9, -0.39, 0.80, -1.14, -2.9, -0.2, -0.6, -0.11, -0.19, 0.07);

    /** Private constructor. */
    private HelmertTransformationFactory() {
    }

}
