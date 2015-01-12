package com.vaadin.vaadininmuija;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.vaadininmuija.akka.UIActor;
import com.vaadin.vaadininmuija.akka.messages.StockQuote;
import com.vaadin.vaadininmuija.akka.messages.UnwatchStock;
import com.vaadin.vaadininmuija.akka.messages.WatchStock;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is the actual Vaadin UI, showing data produced by Akka actors.
 * <p>
 * The interesting thing in the demo is the uiActor of type UIActor. StockUI
 * instantiates one for itself and shuts it down when detached. During activity,
 * the UIActor, calls methods in this class to update the ui.
 * <p>
 * The code also communicates with the hub actor, telling which stocks the
 * uiActor should watch/unwatch.
 *
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Push
@Title("Vaadin( )in Akka")
public class StockUI extends UI {

    final VerticalLayout layout = new VerticalLayout();

    private final Label msg = new Label("<em>The demo backend generates random stock "
            + "prices at random time. Last 50 points per symbol are "
            + "displayed at once.</em>",
            ContentMode.HTML);

    private ActorRef uiActor;

    public static final String[] STOCKS = {"GOOG", "AAPL", "ORCL"};

    @Override
    protected void init(VaadinRequest request) {

        // Create an actor that mediates the data from the "actor system" to 
        // this particular UI instance
        uiActor = getSystem().actorOf(Props.create(UIActor.class, StockUI.this));

        layout.setMargin(true);
        layout.setSpacing(true);
        configureChart();

        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);
        for (String symbol : STOCKS) {
            CheckBox checkBox = new CheckBox(symbol);
            checkBox.addValueChangeListener(e -> {
                Object msg = checkBox.getValue()
                        ? new WatchStock(symbol) : new UnwatchStock(symbol);
                getHub().tell(msg, uiActor);
                if (!checkBox.getValue()) {
                    hideSymbol(symbol);
                }
            });
            checkBox.setValue(true);
            controls.addComponent(checkBox);
        }

        TextField customSymbol = new TextField();
        customSymbol.setWidth("6em");
        customSymbol.setInputPrompt("CSTM");
        CheckBox customToggle = new CheckBox("Follow custom stock:");
        customToggle.addValueChangeListener(e -> {
            String symbol = customSymbol.getValue();
            if (symbol.length() < 2) {
                if (customToggle.getValue()) {
                    Notification.show("Fill valid stock symbol!");
                    customToggle.setValue(false);
                }
            } else {
                Object msg = customToggle.getValue()
                        ? new WatchStock(symbol) : new UnwatchStock(symbol);
                getHub().tell(msg, uiActor);
                customSymbol.setEnabled(!customToggle.getValue());
                if (!customToggle.getValue()) {
                    hideSymbol(symbol);
                }
            }
        });
        controls.addComponents(customToggle, customSymbol);

        layout.addComponents(controls, msg);
        setContent(layout);

        // TODO add sell/buy suggestions
    }

    /**
     * This is the public API for UIActor to update the UI
     *
     * @param stockSymbol
     * @param values
     */
    public void updateStockDetails(String stockSymbol, StockQuote... values) {
        // wrap modification into UI.access(Runnable), as the calls comes from
        // "non UI thread", this is the "hard" part of the Vaadin( )in Akka
        // integration
        access(() -> {
            for (StockQuote value : values) {
            }
        });
    }

    private ActorSystem getSystem() {
        // Fetch the ActorSystem reference from servlet, in real life apps,
        // you'll probably inject this using Spring or CDI
        StockServlet servlet = (StockServlet) ((VaadinServletService) getSession().
                getService()).getServlet();
        return servlet.getSystem();
    }

    public ActorRef getHub() {
        // Fetch the hub in real life apps,
        // you'll probably inject this using Spring or CDI
        StockServlet servlet = (StockServlet) ((VaadinServletService) getSession().
                getService()).getServlet();
        return servlet.getStocksWatch();
    }

    private void hideSymbol(String symbol) {
    }

    private void configureChart() {
    }

    @Override
    public void detach() {
        // Stop ui actor
        getSystem().stop(uiActor);
        super.detach();
    }

}
