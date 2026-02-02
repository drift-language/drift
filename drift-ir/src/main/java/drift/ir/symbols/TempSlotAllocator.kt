/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.symbols


class TempSlotAllocator(
    private val prefix: String) {

    private var slot: Int = 0


    fun allocate() = "$prefix.${slot++}"
}