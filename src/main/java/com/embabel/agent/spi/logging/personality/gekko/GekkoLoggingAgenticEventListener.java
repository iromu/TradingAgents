package com.embabel.agent.spi.logging.personality.gekko;

import com.embabel.agent.event.*;
import com.embabel.agent.event.logging.LoggingAgenticEventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("gekko")
public class GekkoLoggingAgenticEventListener extends LoggingAgenticEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("gekko");

    private static final String WELCOME_MESSAGE = color(
            """
                    
                    
                                                                  ........                                                                                     
                                                          .','..  ..  .........  .                                                                             
                                                        .'''....    .   ....  ......                                                                           
                                                       ....             .     .    . ..                                                                        
                                                    ......               .           ....                                                                      
                                                   ..... ..                            ...                                                                     
                                                   ...                    ..            ...         .';;;;;;;.   .;;;;;;;;'    .;;;;;;;;;. .;;;;;;;;;' .,;;;;;;;
                                                  ..                                    .....     .lkOKOxxxOKk:. ;0K0xxxk00d'  ;0K0kxxxxx; ;0K0kxxddxc.'OK0Oxxk0
                                                  .            .            ..           ..,.     :0KKk;  .:OKk' ;OKd.  .l00l  ;OKk'       ;OKk,       'kKO:  .d0Ko.
                                                 ..         . ......',,...,,',;;,..... ....'.     :00Kx.   .';'  ;OKd.   ;OKl  ;OKk'       ;OKk'       'kKO;   .l0Ko
                                                          ......',;cloododxxddkOkxdolc,';,''.     :0KKx.         ;OKx;..'o00c  ;0KO:'''.   ;OKO:'''.   'kKO;   .l0Ko
                                                          .....',;:codxkkkOO0000000OOOkxdc,'.     :0KKx. ;doodl. ;0K0000000l.  ;0K00OO0d.  ;OK0OOOOl.  'kKO;   .l0Ko
                                                           ....,;;:lodxkkkOOOOO000OOkOOkko;;.     :0KKx. ,cd0Kk' ;OKx;',lk0d.  ;OKk:....   ;OKO:....   'kKO,   .l0Ko
                                              ...          ...';;;:ccldxkOOkkOOO0OOOOOOkkd:;.     :0KKx.   ,OKk' ;OKo.   lK0c  ;OKk.       ;OKk'       'kKO;   .l0Ko
                                               ..          ...',,;:::cdxxkkkkOkkOOkOOOOOOl'..     :00Kk,  .cOKk' ;0Ko.   lK0l  ;OKk,       ;OKk,       'kKO;  .d0Ko.
                                                          .....,,;clooddxxxxkOOOOOOOOkkkOc.       .okOKOxdx0Kk:. ;0Ko.   lKKl  ;0K0kxxxxx; ;0K0kddddxc.'OK0kxxx0
                                               ...       ...'..';:lldxxxkxkOOOkOOkOOOOOkk:          .,:::::::.   .;:'    '::'  .:::::::::' .;::::::::' .;:::::::
                                              .....     ...''.',;:cclloodxkkOkxkOOOkkkkkk;                                                                     
                                              ......   ....''...'......,;:coddlodddoodxxd,                                .,;'   .,::::::,.                    
                                             ........  .','.'. .          .;llc;;'....,;:.                                :0Kd. .d0KOxxk00x'                   
                                             ......'....,;;:,....'..,o:....'lxo'.. .,..,;.                                :0Kd. :0Kx'...l00:                   
                                              .'....'...'';oool:,,;,:lc;,..,dOxlc:,:ddld:                                 :0Kd. :0Kd.   ....                   
                                               .','..'''..':oxkxdll::cc:;,',dOkdolccdkkk;                                 :0Kd. ,k0Od:'.                       
                                                ..,,',;,...,:ldkOkxdddcc:,,,lkOOOkdxkkkd'                                 :0Kd.  .;oO00koc'                    
                                                  ...,;'....';odxxxkkxo:;,',lkOkkkkkkkkc.                                 :0Kd.     .,cx0KO;                   
                                                   ..,,,'..'',cooooxdl;'''',okkkkkkkxkd'                                  :0Kd. .;;.   .oK0:                   
                                                   .',,,'''',,:cllldo:'...';lxkkkkkkxkl.                                  :0Kd. cKKd'..,xK0:                   
                                                  ,:.','','';:ccclodol;.....':dxkkkxxo'                                   :0Kd. .oO0OOO00Ol.                   
                                                 .oo,.....'.',:cc::::::;,',';lddxkxddc                                    .,,.    .,,,,,,.                     
                                                 'dxc.........:cc:'..''.',;:clolodxxo'                                                                         
                                                 .lxo,........,:cc:;,,;,,:::cloloodl'                      ,cccccc;.    .,cccccc,.    .;cccccc;.   .ccccccc:'  
                                                  ,ddl;'.........,;;,''.'';;;coddl,.                     'd00kxdk00x,  ;x00kddk00x,  ,x00kddx00k:. :0K0xdxO0Oo'
                                                  .cdddd:.. .  ...':clllllddlooo;.                       l0Kx.  .oKKo..dK0o.  .l0Kx..oK0d.  .c0Kk' :0Kd.  .d0Ko.
                                                   'oxddooc'.     ..':::c::ooc:.                         lKKo.   '::, .dK0:    :0Kx..oK0l    ;OKk' :0Kd.   l0Ko
                                                    ,dxdodxdoc'. .......'',:l;.                          l0Ko.        .dK0:    :0Kx..oK0l    ;OKk' :0Kd.   l0Ko
                                                     ,dxddxxxddl:'......':ox0o.                          l0Ko..ckxxxl..dK0:    :0Kx..oK0l    ;OKk' :0Kd.   l0Ko
                                                      :xxxxkkxkkxdl,..,:d0000o. .                        l0Ko. ':d0Kd..dK0:    :0Kx..oK0l    ;OKk' :0Kd.   l0Ko
                                                      .:kkkkkkkkkxkd;.';:oO00d. ..   .                   l0Ko.   :0Kd..dK0:    :0Kx..oK0l    ;OKk' :0Kd.   l0Ko
                                                       .lkkkkkkkkxo'  ....'d0x'   ..  .                  l0Kk;..'d0Kd..dK0d'..'d0Kd..oK0x,..'o0Kk. :0Kx,..;x0Kl
                                                        .lkkkkkkd;.    .....lk;   ..                     .ck00OO00Ol.  .lO0OOOO0Ol.  .lOK0OO0KOo'  :000OOO00x:.
                                                         'xOkOOo,',.    .;l;'ll.     ..           .        .',,,,,.      .,,''',.      .,,,,,,'    .,,',,','.  
                                                          :kOkxdl:;'.   .ckxolo:     .                                                                         
                                                          .:ddxkdc,.. .  ,xOOkdc.                                                                              
                                                           .cxxxxo,.     .lOOOko.                                                                              
                                                            ,dkxdo,       'dkkkx,                                                                              
                                                             ,xkxo,.    . .:xkkkc.                                                                             
                                                              :kkd:.       .;dkko.                                                                             
                                                              .okxc. ..   .  ;kkx'                                                                             
                                                               'dx;  ..   .  .oOx'                                                                             
                                                                ,d;  ..       ;kk;                                                                             
                                                                 ;;  .        .lk:                                                                             
                                                                 ... .        .,d:                                                                             
                                                                  . ..  . ..  ..cc                                                                             
                                                                                .'      
                    """,
            GekkoColorPalette.PROFIT_GREEN
    );

    public GekkoLoggingAgenticEventListener(GekkoColorPalette palette) {
        super(null, WELCOME_MESSAGE, LOGGER, palette);
    }

    @Override
    public String getAgentDeploymentEventMessage(AgentDeploymentEvent e) {
        return String.format("Deployed agent: %s - Ready to make profit. Description: %s",
                e.getAgent().getName(), e.getAgent().getDescription());
    }

    @Override
    public String getRankingChoiceMadeEventMessage(RankingChoiceMadeEvent<?> e) {
        return String.format("Picked %s with confidence %.2f based on %s. Let's capitalize on it.",
                e.getType().getSimpleName(), e.getChoice().getScore(), e.getBasis());
    }

    @Override
    public String getDynamicAgentCreationMessage(DynamicAgentCreationEvent e) {
        return indentLines(
                "Created a new agent instance to maximize returns:\n" +
                        e.getAgent().infoString(true, 1), 1, true);
    }

    @Override
    public String getAgentProcessCreationEventMessage(AgentProcessCreationEvent e) {
        return String.format("Process created: %s - Time to execute and profit.", e.getProcessId());
    }

    @Override
    public String getAgentProcessReadyToPlanEventMessage(AgentProcessReadyToPlanEvent e) {
        return indentLines(
                String.format("[%s] Ready to strategize and take advantage of opportunities:\n%s",
                        e.getProcessId(),
                        e.getWorldState().infoString(
                                e.getAgentProcess().getProcessContext().getProcessOptions().getVerbosity().getShowLongPlans(),
                                1
                        )), 1, true);
    }

    @Override
    public String getAgentProcessPlanFormulatedEventMessage(AgentProcessPlanFormulatedEvent e) {
        return indentLines(
                String.format("[%s] Plan formulated to dominate the market:\n%s\nBased on current state:\n%s",
                        e.getProcessId(),
                        e.getPlan().infoString(
                                e.getAgentProcess().getProcessContext().getProcessOptions().getVerbosity().getShowLongPlans(),
                                1
                        ),
                        e.getWorldState().infoString(true, 1)
                ), 1, true);
    }

    @Override
    public String getProcessCompletionMessage(AgentProcessFinishedEvent e) {
        return String.format("[%s] Mission complete. Profit realized in %s.",
                e.getProcessId(), e.getAgentProcess().getRunningTime());
    }

    @Override
    public String getProcessFailureMessage(AgentProcessFinishedEvent e) {
        return String.format("[%s] Opportunity missed. The market bites back.", e.getProcessId());
    }

    @Override
    public String getObjectAddedEventMessage(ObjectAddedEvent e) {
        String value = e.getAgentProcess().getProcessContext().getProcessOptions().getVerbosity().getDebug() ?
                e.getValue().toString() : e.getValue().getClass().getSimpleName();
        return String.format("Asset added: %s to process %s. Maximize the value.", value, e.getProcessId());
    }

    @Override
    public String getLlmRequestEventMessage(LlmRequestEvent<?> e) {
        return String.format("[%s] (%s) Consulting LLM %s to gain the edge. Producing: %s",
                e.getProcessId(), e.getInteraction().getName(),
                e.getLlm().getName(), e.getOutputClass().getSimpleName());
    }

    @Override
    public String getActionExecutionStartMessage(ActionExecutionStartEvent e) {
        return String.format("[%s] Executing action %s. No hesitation, just results.",
                e.getProcessId(), e.getAction().getName());
    }

    @Override
    public String getActionExecutionResultMessage(ActionExecutionResultEvent e) {
        return String.format("[%s] Action %s completed in %s. Profit potential evaluated.",
                e.getProcessId(), e.getAction().getName(), e.getActionStatus().getRunningTime());
    }

    @Override
    public String getObjectBoundEventMessage(ObjectBoundEvent e) {
        String value = e.getAgentProcess().getProcessContext().getProcessOptions().getVerbosity().getDebug() ?
                e.getValue().toString() : e.getValue().getClass().getSimpleName();
        return String.format("[%s] Asset secured: %s:%s. Keep it close, maximize gains.",
                e.getProcessId(), e.getName(), value);
    }

    @NotNull
    @Override
    protected String getToolCallSuccessResponseEventMessage(@NotNull ToolCallResponseEvent e, @NotNull String resultToShow) {
        return super.getToolCallSuccessResponseEventMessage(e, "");
    }

    private static String indentLines(String text, int level, boolean skipFirstLine) {
        String indent = "    ".repeat(level);
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && skipFirstLine) {
                sb.append(lines[i]).append("\n");
            } else {
                sb.append(indent).append(lines[i]).append("\n");
            }
        }
        return sb.toString();
    }

    private static String color(String text, int colorCode) {
        return text; // Placeholder for colorizing if needed
    }
}
