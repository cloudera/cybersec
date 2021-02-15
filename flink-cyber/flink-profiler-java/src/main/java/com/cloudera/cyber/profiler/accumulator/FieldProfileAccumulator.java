package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.text.DecimalFormat;

@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public abstract class FieldProfileAccumulator extends DoubleProfileAccumulator {

     private String fieldName;

     public FieldProfileAccumulator(String resultExtensionName, String fieldName, double initialValue, DecimalFormat format) {
         super(resultExtensionName, initialValue, format);
         this.fieldName = fieldName;
     }

     public String getFieldName() {
         return fieldName;
     }

    @Override
    public void add(Message message) {
        Double value = getFieldValueAsDouble(message);
        if (value != null) {
            updateProfileValue(value);
        }
    }

    protected abstract void updateProfileValue(double fieldValue);

    protected Double getFieldValueAsDouble(Message message) {
         String extensionValue = message.getExtensions().get(fieldName);
         if (extensionValue != null) {
             try {
                 return Double.parseDouble(extensionValue);
             } catch (NumberFormatException ignored){

             }
         }
         return null;
     }
}
