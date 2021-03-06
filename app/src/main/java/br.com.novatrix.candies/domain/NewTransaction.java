package br.com.novatrix.candies.domain;

import com.google.gson.annotations.SerializedName;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class NewTransaction
{
    @SerializedName("status")
    private String status;

    @SerializedName("return")
    private String message;

    public String getMessage() {
        return message;
    }

    public boolean isSuccessfull() {
        try {
            int statusCode = Integer.parseInt(status);
            return statusCode > 0;
        } catch (Exception exception) {
            return false;
        }
    }
}
