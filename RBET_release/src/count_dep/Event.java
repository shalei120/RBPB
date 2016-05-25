/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import AceJet.AceEvent;
import AceJet.AceEventMention;
import AceJet.AceEventMentionArgument;
import java.util.LinkedList;
import java.util.List;

public class Event {

    public String span;
    public String trigger, eventtype;
    public List<EventArgument> arguments;
    public String filename;
    public String eventlargetype;

    public Event() {
        arguments = new LinkedList<>();
    }
    public Event(AceEvent ae){
        this();
        AceEventMention get = ae.mentions.get(0);
        this.trigger=get.anchorText;
        this.span=get.text;
        this.eventlargetype=ae.type;
        this.eventtype=ae.subtype;
        for(AceEventMentionArgument aema : get.arguments){
            EventArgument ea=new EventArgument(aema);
            arguments.add(ea);
        }
    }
    

}
