package GuiElementTypeInferring;

import Layout.Attribute;
import Layout.AttributeType;
import Layout.GuiElement;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.validator.routines.*;

/**
 * The GuiElementTypeInferring class encapsulates different heuristics , albeit rudimentary,
 * that tries to infer the type of a GuiElement based on attributes that either
 * dictate the type of the data or hint at the type of the data.
 * Moreover it performs pattern matching on the string values held by certain attributes.
 */
public class GuiElementTypeInferring {

    private GuiElement guiElement;
    private Attribute<?> textAttribute;
    private Attribute<?> passwordAttribute;
    private Attribute<?> autoLinkAttribute;
    private Attribute<?> inputTypeAttribute;
    private Attribute<?> digitsAttribute;

    public GuiElementTypeInferring(GuiElement guiElement) {
        this.guiElement = guiElement;
        this.textAttribute = this.guiElement.getAttribute("text");
        this.passwordAttribute = this.guiElement.getAttribute("password");
        this.autoLinkAttribute = this.guiElement.getAttribute("autoLink");
        this.inputTypeAttribute = this.guiElement.getAttribute("inputType");
        this.digitsAttribute = this.guiElement.getAttribute("digits");
    }

    /**
     * This method checks uses different heuristics to try and infer
     * the type of data a Gui Element may hold
     *
     * @return returns the type that was inferred
     */
    public GuiElementInferredDataType getInferredDataHoldingType() {

        // if none of the attributes are defined on the Gui Element that we use to infer the
        // type, then return some default value
        if (passwordAttribute == null && inputTypeAttribute == null && !doesAutoLinkAttributeExist() && !doesTextAttributeExist()) {
            if (this.guiElement.getTag().contains("TextView"))
                return GuiElementInferredDataType.STRING;
            else if (this.guiElement.getTag().contains("EditText"))
                return GuiElementInferredDataType.STRING;
            else
                return GuiElementInferredDataType.HOLDS_NO_DATA;
        }

        // Input type resolving for edit text gui elements
        if (inputTypeAttribute != null) {
            Integer inputType = (Integer) inputTypeAttribute.getValue();
            switch (inputType) {
                case 1:
                case 4097:
                case 8193:
                case 16385:
                case 65537:
                case 32769:
                case 262145:
                case 131073:
                case 524289:
                case 49:
                case 177:
                case 81:
                case 97:
                case 193:
                case 65:
                case 161:
                    // TYPE_CLASS_TEXT, TYPE_TEXT_FLAG_CAP_CHARACTERS, TYPE_TEXT_FLAG_CAP_WORDS, TYPE_TEXT_FLAG_CAP_SENTENCES
                    // TYPE_TEXT_FLAG_AUTO_COMPLETE, TYPE_TEXT_FLAG_AUTO_CORRECT, TYPE_TEXT_FLAG_IME_MULTI_LINE
                    // TYPE_TEXT_FLAG_MULTI_LINE, TYPE_TEXT_FLAG_NO_SUGGESTIONS, TYPE_TEXT_VARIATION_EMAIL_SUBJECT
                    // TYPE_TEXT_VARIATION_FILTER, TYPE_TEXT_VARIATION_LONG_MESSAGE, TYPE_TEXT_VARIATION_PERSON_NAME
                    // TYPE_TEXT_VARIATION_PHONETIC, TYPE_TEXT_VARIATION_SHORT_MESSAGE, TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
                    return GuiElementInferredDataType.STRING;
                case 17:
                    // TYPE_TEXT_VARIATION_URI
                    return GuiElementInferredDataType.URI;
                case 2:
                case 4098:
                case 8194:
                    // TYPE_CLASS_NUMBER, TYPE_NUMBER_FLAG_SIGNED, TYPE_NUMBER_FLAG_DECIMAL
                    return GuiElementInferredDataType.NUMERIC;
                case 3:
                    // TYPE_CLASS_PHONE
                    return GuiElementInferredDataType.PHONE_NUMBER;
                case 4:
                case 20:
                case 36:
                    // TYPE_CLASS_DATETIME, TYPE_DATETIME_VARIATION_DATE, TYPE_DATETIME_VARIATION_TIME
                    return GuiElementInferredDataType.DATE;
                case 18:
                case 129:
                case 145:
                case 225:
                    // TYPE_NUMBER_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    // TYPE_TEXT_VARIATION_WEB_PASSWORD
                    return GuiElementInferredDataType.PASSWORD;
                case 33:
                case 209:
                    // TYPE_TEXT_VARIATION_EMAIL_ADDRESS, TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    return GuiElementInferredDataType.EMAIL_ADDRESS;
                case 113:
                    // TYPE_TEXT_VARIATION_POSTAL_ADDRESS
                    return GuiElementInferredDataType.ADDRESS;
                // Do not return STRING by default
                // Let the other heuristics below conclude
            }
        }

        // Check for the presence and setting of the password attribute
        if (passwordAttribute != null && ((Boolean) passwordAttribute.getValue()))
            return GuiElementInferredDataType.PASSWORD;

        // Check for the presence and setting of the autoLink attribute
        if (autoLinkAttribute != null) {
            String value = ((String) autoLinkAttribute.getValue());

            if (value.equals("email"))
                return GuiElementInferredDataType.EMAIL_ADDRESS;
            if (value.equals("web"))
                return GuiElementInferredDataType.URL;
            if (value.equals("phone"))
                return GuiElementInferredDataType.PHONE_NUMBER;

        }

        // String matching on the text Attribute
        if (holdsCreditCard())
            return GuiElementInferredDataType.CREDIT_CARD;
        if (holdsDate())
            return GuiElementInferredDataType.DATE;
        if (holdsEmail())
            return GuiElementInferredDataType.EMAIL_ADDRESS;
        if (holdsEmail())
            return GuiElementInferredDataType.IBAN;
        if (holdsUrl())
            return GuiElementInferredDataType.URL;
        if (holdsIPAddress())
            return GuiElementInferredDataType.IP_ADDRESS;
        if (holdsPhoneNumber())
            return GuiElementInferredDataType.PHONE_NUMBER;
        if (holdsPassword())
            return GuiElementInferredDataType.PASSWORD;
        if (holdsIBAN())
            return GuiElementInferredDataType.IBAN;
        if (holdsNumeric())
            return GuiElementInferredDataType.NUMERIC;

        return GuiElementInferredDataType.STRING;

    }

    private boolean holdsDate() {

        if (!isTextAttributeString())
            return false;

        String value = getTextAttributeValue();

        return doesTextAttributeExist() && new DateValidator().isValid(value);
    }

    private boolean holdsCreditCard() {

        if (!doesTextAttributeExist())
            return false;

        String value = getTextAttributeValue().replaceAll("-","");

        return new CreditCardValidator().isValid(value);
    }

    private boolean holdsEmail() {

        if (doesAutoLinkAttributeExist()) {
            String value = (String) autoLinkAttribute.getValue();

            if (value.equals("email"))
                return true;
        }
        if (!isTextAttributeString())
            return false;
        else {
            String value = getTextAttributeValue();
            return EmailValidator.getInstance().isValid(value);
        }

    }

    private boolean holdsUrl() {

        if (doesAutoLinkAttributeExist()) {
            String value = (String) autoLinkAttribute.getValue();

            if (value.equals("web"))
                return true;
        }
        if (!isTextAttributeString())
            return false;
        else {
            String value = getTextAttributeValue();
            return UrlValidator.getInstance().isValid(value);
        }

    }

    private boolean holdsIBAN() {

        if (!isTextAttributeString())
            return false;

        String value = getTextAttributeValue();

        return doesTextAttributeExist() && IBANValidator.getInstance().isValid(value);
    }

    private boolean holdsNumeric() {

        if (digitsAttribute != null)
            return true;

        if (!doesTextAttributeExist())
            return false;

        String value = getTextAttributeValue();

        return IntegerValidator.getInstance().isValid(value) || FloatValidator.getInstance().isValid(value);
    }

    private boolean holdsIPAddress() {

        if (!isTextAttributeString())
            return false;

        String value = getTextAttributeValue();

        return doesTextAttributeExist() && InetAddressValidator.getInstance().isValid(value);
    }

    private boolean holdsPhoneNumber() {

        if (doesAutoLinkAttributeExist()) {
            String value = (String) autoLinkAttribute.getValue();

            if (value.equals("phone"))
                return true;
        }
        if (!isTextAttributeString())
            return false;
        else {
            String value = getTextAttributeValue();
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

            try {
                Phonenumber.PhoneNumber phone = phoneNumberUtil.parse(value, Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
                return phoneNumberUtil.isValidNumber(phone);
            } catch (Exception e) {
                return false;
            }
        }

    }

    private boolean holdsPassword() {

        if (!doesTextAttributeExist())
            return false;

        return passwordAttribute != null && ((Boolean) passwordAttribute.getValue());
    }

    private boolean doesTextAttributeExist() {
        return this.textAttribute != null;
    }

    private boolean doesAutoLinkAttributeExist() {
        return this.autoLinkAttribute != null;
    }

    private boolean isTextAttributeString() {
        return doesTextAttributeExist() && (this.textAttribute.getType() == AttributeType.STRING || this.textAttribute.getLocalizedStringValue() != null);
    }

    private String getTextAttributeValue() {

        String value = "";

        if (textAttribute.getType() == AttributeType.REFERENCE) {
            value = textAttribute.getLocalizedStringValue();
        } else if (textAttribute.getType() == AttributeType.INT) {
            value = ((Integer) textAttribute.getValue()).toString();
        } else {
            if (textAttribute.getValue() instanceof Integer)
                value = ((Integer) textAttribute.getValue()).toString();
            else
                value = ((String) textAttribute.getValue());
        }

        if (value == null)
            value = "";
        return value;
    }

}
