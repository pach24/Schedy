package com.schednd.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCodeGeneratorTest {

    @Test
    fun `el codigo generado tiene seis caracteres`() {
        repeat(50) {
            assertEquals(6, EventCodeGenerator.generate().length)
        }
    }

    @Test
    fun `el alfabeto excluye caracteres que se confunden al dictarlos`() {
        // Sin I, O, 0 ni 1: el código se comparte por voz y por WhatsApp.
        val prohibidos = setOf('I', 'O', '0', '1')
        repeat(200) {
            val codigo = EventCodeGenerator.generate()
            assertTrue(
                "El código $codigo contiene un carácter ambiguo",
                codigo.none { it in prohibidos }
            )
        }
    }

    @Test
    fun `no repite el mismo codigo una vez tras otra`() {
        // No prueba la aleatoriedad: solo que no devuelve una constante.
        val codigos = (1..100).map { EventCodeGenerator.generate() }.toSet()
        assertTrue("Generó demasiados repetidos: ${codigos.size} únicos de 100", codigos.size > 95)
    }

    @Test
    fun `valida el formato de un codigo`() {
        assertTrue(EventCodeGenerator.isValid("ABC234"))
        assertTrue(EventCodeGenerator.isValid(EventCodeGenerator.generate()))
    }

    @Test
    fun `rechaza longitudes distintas de seis`() {
        assertFalse(EventCodeGenerator.isValid("ABC23"))
        assertFalse(EventCodeGenerator.isValid("ABC2345"))
        assertFalse(EventCodeGenerator.isValid(""))
    }

    @Test
    fun `rechaza minusculas y caracteres fuera del alfabeto`() {
        assertFalse(EventCodeGenerator.isValid("abc234"))
        assertFalse(EventCodeGenerator.isValid("ABC-34"))
        assertFalse(EventCodeGenerator.isValid("ABCI34"))
        assertFalse(EventCodeGenerator.isValid("ABC034"))
    }
}
