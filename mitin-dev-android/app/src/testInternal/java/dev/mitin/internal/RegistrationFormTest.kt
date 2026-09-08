package dev.mitin.internal

import org.junit.Assert.*
import org.junit.Test

class RegistrationFormTest {
    private val password="Synthetic password 123"
    private fun valid(mode:String="register", email:String="a@example.test", name:String="Client", p:String=password,
                      repeat:String=p, token:String="A".repeat(43), consent:Boolean=true, terms:Boolean=true) =
        validAuthForm(mode,email,name,p,repeat,token,consent,terms)
    @Test fun registrationRequiresBothConsentsAndMatchingPasswords() {
        assertTrue(valid());assertFalse(valid(consent=false));assertFalse(valid(terms=false))
        assertFalse(valid(repeat="different"));assertFalse(valid(name="  "))
    }
    @Test fun boundedEmailAndPassword() {
        assertFalse(valid(email="invalid"));assertFalse(valid(email="a\nb@example.test"))
        assertFalse(valid(p="short"));assertFalse(valid(p="a".repeat(129)))
        assertTrue(valid(p="😀".repeat(15)))
    }
    @Test fun verificationAndResetHavePurposeSpecificInputs() {
        assertTrue(valid(mode="verify-email",p=""))
        assertFalse(valid(mode="verify-email",token="A".repeat(42)))
        assertTrue(valid(mode="password-reset/confirm"))
        assertFalse(valid(mode="password-reset/confirm",repeat="bad"))
        assertTrue(valid(mode="password-reset/request",p="",token=""))
        assertFalse(valid(mode="unknown"))
    }
}
