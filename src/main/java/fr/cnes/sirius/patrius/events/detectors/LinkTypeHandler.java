/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.14:OPENFD-179:22/08/2024: [PATRIUS] Gestion emetteur/recepteur dans les detecteurs d'evenements
 * VERSION:4.14:OPENFD-343:22/08/2024: Ajout de regles de codage dans le standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.detectors;

import java.io.Serializable;

import fr.cnes.sirius.patrius.events.detectors.SatToSatMutualVisibilityDetector.SatToSatLinkType;
import fr.cnes.sirius.patrius.events.detectors.VisibilityFromStationDetector.LinkType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;

/**
 * Define the role of the main element (SpacecraftState) in the signal propagation (emitter or receiver) and the other
 * element.
 * 
 * @author tbonit
 * 
 * @since 4.14
 */
public class LinkTypeHandler implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 3743083680266927069L;

    /** Signal propagation role. */
    public enum SignalPropagationRole {

        /** The element has the role of emitter in the signal propagation. */
        EMITTER,

        /** The element has the role of receiver in the signal propagation. */
        RECEIVER;
    }

    /** Define the role of the main element (SpacecraftState) in the signal propagation (emitter or receiver). */
    private final SignalPropagationRole mainRole;

    /** Other element involved in the signal propagation. */
    private final PVCoordinatesProvider otherElement;

    /**
     * Constructor with a {@link LinkType}.
     * 
     * @param linkType
     *        Type of link (it can be uplink or downlink)
     * @param otherElement
     *        Other element involved in the signal propagation
     */
    public LinkTypeHandler(final LinkType linkType, final PVCoordinatesProvider otherElement) {
        // Note: null linkType case managed by the AbstractSignalPropagationDetector class
        this(linkType == LinkType.DOWNLINK ? SignalPropagationRole.EMITTER : SignalPropagationRole.RECEIVER,
                otherElement);
    }

    /**
     * Constructor with a {@link SatToSatLinkType}.
     * 
     * @param linkType
     *        Type of link (it can be secondary to main or main to Secondary)
     * @param otherElement
     *        Other element involved in the signal propagation
     */
    public LinkTypeHandler(final SatToSatLinkType linkType, final PVCoordinatesProvider otherElement) {
        // Note: null linkType case managed by the AbstractSignalPropagationDetector class
        this(linkType == SatToSatLinkType.MAIN_TO_SECONDARY ? SignalPropagationRole.EMITTER
            : SignalPropagationRole.RECEIVER, otherElement);
    }

    /**
     * Constructor with a {@link SignalPropagationRole}.
     * 
     * @param mainRole
     *        Define the role of the main element (SpacecraftState) in the signal propagation (emitter or receiver)
     * @param otherElement
     *        Other element involved in the signal propagation
     */
    public LinkTypeHandler(final SignalPropagationRole mainRole, final PVCoordinatesProvider otherElement) {
        this.mainRole = mainRole;
        this.otherElement = otherElement;
    }

    /**
     * Getter for the role of the main element (SpacecraftState) in the signal propagation (emitter or receiver).
     * 
     * @return the role of the main element
     */
    public SignalPropagationRole getMainRole() {
        return this.mainRole;
    }

    /**
     * Getter for the other element involved in the signal propagation.
     * 
     * @return the other element involved in the signal propagation
     */
    public PVCoordinatesProvider getOtherElement() {
        return this.otherElement;
    }
}
