/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import AceJet.AceEventMentionArgument;

/**
 *
 * @author v-lesha
 */
public class EventArgument {
    public String data;
    public String type;

    public EventArgument() {
    }

    public EventArgument(String data, String type) {
        this.data = data;
        this.type = type;
    }
    public EventArgument(AceEventMentionArgument aema){
        this.data=aema.value.text;
        this.type=aema.role;
    }
    
}
