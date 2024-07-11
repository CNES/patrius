/**
 * 
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.propagation;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * A simple dummy propagator used for some test purpose.
 * </p>
 * <p>
 * This DummyPropagator stores a PVCoordinates, a date and a frame. It does nothing but returns stored PVCoordinates at
 * stored date and in stored frame. It has been created to provide a non-time based propagator.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Sylvain VRESK
 * 
 * @version $Id: DummyPropagator.java 17926 2017-09-11 13:54:24Z bignon $
 * 
 * 
 */
public class DummyPropagator implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6626085125800203324L;

    /**
     * The stored PVCoordinates.
     */
    private final PVCoordinates pvc;

    /**
     * The stored AbsoluteDate.
     */
    private final AbsoluteDate date;

    /**
     * The stored frame.
     */
    private final Frame frame;

    /**
     * Creates a new instance.
     * 
     * @param pvc
     *        the PVCoordinates
     * @param date
     *        the date
     * @param frame
     *        the frame
     * 
     * @since 1.0
     */
    public DummyPropagator(final PVCoordinates pvc, final AbsoluteDate date, final Frame frame) {
        super();
        this.pvc = pvc;
        this.date = date;
        this.frame = frame;
    }

    /**
     * Dummy propagator.
     */
    public PVCoordinates getPVCoordinates() {
        return this.pvc;
    }

    /**
     * Dummy propagator.
     */
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Dummy propagator.
     */
   public Frame getFrame() {
        return this.frame;
    }

    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        if (date.compareTo(this.date) != 0) {
            throw new IllegalArgumentException();
        }
        if (!frame.equals(this.frame)) {
            throw new IllegalArgumentException();
        }
        return this.pvc;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.frame;
    }
}
