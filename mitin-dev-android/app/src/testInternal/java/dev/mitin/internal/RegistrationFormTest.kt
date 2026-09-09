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
    @Test fun sixCharacterBoundaryAndReportedTenCharacterPassword() {
        for(mode in listOf("register","login","password-reset/confirm")) {
            assertFalse(valid(mode=mode,p="a".repeat(5)))
            assertTrue(valid(mode=mode,p="a".repeat(6)))
            assertTrue(valid(mode=mode,p="a".repeat(10)))
            assertTrue(valid(mode=mode,p="a".repeat(128)))
            assertFalse(valid(mode=mode,p="a".repeat(129)))
        }
        assertNotNull(passwordError("short"))
        assertNotNull(passwordError("\uD800".repeat(6)))
        assertNull(passwordError("😀".repeat(6)))
    }
    @Test fun optionalPhoneCannotBlockButInvalidPhoneExplainsWhy() {
        for(phone in listOf("", "  ", "+7 (999) 123-45-67")) {
            assertNull(phoneError(phone))
            assertTrue(validAuthForm("register","a@example.test","Client",password,password,"",true,true,phone))
        }
        for(phone in listOf("phone", "+123", "1".repeat(16), "++79991234567")) {
            assertNotNull(phoneError(phone))
            assertFalse(validAuthForm("register","a@example.test","Client",password,password,"",true,true,phone))
        }
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
