package catgirlroutes.config.jsonutils


import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.DummySetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.StringSetting
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type

class SettingDeserializer: JsonDeserializer<Setting<*>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Setting<*> {
        if (json?.isJsonObject == true) {
            if (json.asJsonObject.entrySet().isEmpty()) return  DummySetting("Undefined")

            /**
             * The JsonObject for a Setting should only have one property. If more properties will be needed, this
             * deserializer has to be updated.
             * For now only the first element is used.
             */
            val name = json.asJsonObject.entrySet().first().key
            val value = json.asJsonObject.entrySet().first().value

            if (value.isJsonPrimitive) {
                when {
                    (value as JsonPrimitive).isBoolean -> return BooleanSetting(name, value.asBoolean)
                    value.isNumber -> return NumberSetting(name, value.asDouble)
                    value.isString -> return StringSetting(name, value.asString)
                }
            }
        }
        return DummySetting("Undefined")
    }
}