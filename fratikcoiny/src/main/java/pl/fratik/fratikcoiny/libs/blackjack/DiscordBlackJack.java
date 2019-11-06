/*
 * Copyright (C) 2019 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.fratikcoiny.libs.blackjack;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MessageWaiter;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DiscordBlackJack {
    private Hand dealerHand;
    private Hand playerHand;
    private Deck theDeck;
    private final Member player;
    private final CommandContext context;
    private long playerMoney;
    private boolean gameOver = false;
    private final EventWaiter eventWaiter;
    private long playerBet;
    private Message embedMessage;
    private Hand winnerHand;
    private int roundCount;

    public DiscordBlackJack(CommandContext context, long playerMoney, EventWaiter eventWaiter) {
        player = context.getMember();
        this.context = context;
        this.playerMoney = playerMoney;
        this.eventWaiter = eventWaiter;
    }

    public BlackjackResult startPlay(long bet) {
        playerBet = bet;
        theDeck = new Deck();
        theDeck.shuffle();
        dealerHand = new Hand("Dealer");
        playerHand = new Hand(player);
        theDeck.deal(dealerHand);
        theDeck.deal(playerHand);
        theDeck.deal(dealerHand);
        theDeck.deal(playerHand);
        reveal();
        while(!gameOver){
            askPlayer();
        }
        return new BlackjackResult(winnerHand.equals(playerHand), getWonHajs());
    }

    private long getWonHajs() {
        long hajs = playerMoney;
        if (winnerHand.equals(playerHand)) hajs += playerBet;
        else hajs -= playerBet;
        return hajs;
    }

    public void askBet() {
        throw new UnsupportedOperationException();
    }

    private void askPlayer() {
        CountDownLatch latch = new CountDownLatch(1);
        MessageWaiter waiter = new MessageWaiter(eventWaiter, context);
        waiter.setMessageHandler(e -> {
            e.getMessage().delete().queue(null, a->{});
            roundCount++;
            if (e.getMessage().getContentRaw().equalsIgnoreCase("hit") && wszystkieAkcjeEnum().contains(Akcje.HIT)) {
                theDeck.deal(playerHand);
                if (playerHand.getNetValue() > 21) {
                    dealerWins();
                }
                else {
                    reveal();
                }
            }
            if (e.getMessage().getContentRaw().equalsIgnoreCase("stand") && wszystkieAkcjeEnum().contains(Akcje.STAND)) {
                while (!gameOver) {
                    dealerDraws();
                }
            }
            if (e.getMessage().getContentRaw().equalsIgnoreCase("surrender") && wszystkieAkcjeEnum().contains(Akcje.SURRENDER)) {
                surrender();
            }
            latch.countDown();
            // TODO: 2019-04-05 logika innych, zabieranie/dodawanie coinow, etc
        });
        waiter.setTimeoutHandler(() -> {
            context.send(context.getTranslated("blackjack.end.of.time"));
            gameOver = true;
            dealerWins(false);
            latch.countDown();
        });
        waiter.create();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void surrender() {
        gameOver = true;
        dealerWins(false);
        context.send(context.getTranslated("blackjack.surrender"));
    }

    private void dealerDraws() {
        if (dealerHand.getNetValue() > 21){
            playerWins();
            reveal();
        }
        else if (dealerHand.getNetValue() <= 16){
            theDeck.deal(dealerHand);
            reveal();
        }
        else {
            if (playerHand.getNetValue() < dealerHand.getNetValue()) {
                dealerWins();
            }
            else if (playerHand.getNetValue() > dealerHand.getNetValue()){
                playerWins();
            }
            else {
                playerWins();
            }
        }
    }

    private void reveal() {
        if (embedMessage == null) embedMessage = context.getChannel().sendMessage(generateEmbed()).complete();
        else embedMessage.editMessage(generateEmbed()).complete();
    }

    private MessageEmbed generateEmbed() {
        EmbedBuilder eb = new EmbedBuilder()
                .setTimestamp(Instant.now())
                .setAuthor("Blackjack | " + context.getSender().getName() + " (" + playerBet + ")")
                .setDescription(context.getTranslated("blackjack.akcje") + "\n" + wszystkieAkcje() + "\n\n" +
                        context.getTranslated("blackjack.akcje.czas"))
                .addField(context.getTranslated("blackjack.karty.twoje"),
                        playerHand.display() + "\n" + context.getTranslated("blackjack.wartosc",
                                playerHand.getNetValue(false)), true);
        if (!gameOver) eb.addField(context.getTranslated("blackjack.karty.krupiera"),
                dealerHand.hiddenDisplay() + "\n" + context.getTranslated("blackjack.wartosc",
                        dealerHand.getNetValue(true)), true);
        else eb.addField(context.getTranslated("blackjack.karty.krupiera"),
                dealerHand.display() + "\n" + context.getTranslated("blackjack.wartosc",
                        dealerHand.getNetValue(false)), true);
        Emote emotkaFc = context.getShardManager().getEmoteById(Ustawienia.instance.emotki.fratikCoin);
        if (emotkaFc == null) throw new IllegalStateException("emotka null");
        if (gameOver && winnerHand.equals(playerHand)) {
            eb.setColor(Color.green);
            eb.setAuthor(context.getTranslated("blackjack.end.won"));
            eb.setDescription(context.getTranslated("blackjack.end.won.desc",
                    emotkaFc.getAsMention(),
                    playerBet, playerMoney + playerBet));
        }
        if (gameOver && winnerHand.equals(dealerHand)) {
            eb.setColor(Color.red);
            eb.setAuthor(context.getTranslated("blackjack.end.lost"));
            eb.setDescription(context.getTranslated("blackjack.end.lost.desc",
                    emotkaFc.getAsMention(),
                    playerMoney - playerBet));
        }
        return eb.build();
    }

    private List<Akcje> wszystkieAkcjeEnum() {
        List<Akcje> list = new ArrayList<>();
        list.add(Akcje.HIT);
        list.add(Akcje.STAND);
        if (roundCount == 0) {
            list.add(Akcje.SURRENDER);
//            list.add(Akcje.DOUBLE);
        }
//        if (dealerHand.getHandContents().get(0).getRank().equals("A")) list.add(Akcje.INSURANCE);
//        if (playerHand.getHandContents().get(0).getValue() == playerHand.getHandContents().get(1).getValue() && roundCount == 1) list.add(Akcje.SPLIT);
        return list;
    }

    private String wszystkieAkcje() {
        StringBuilder sb = new StringBuilder();
        wszystkieAkcjeEnum().forEach(a -> sb.append("`").append(a.name().toLowerCase()).append("`\n"));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private void endReveal() {
        endReveal(true);
    }

    private void endReveal(boolean announce) {
        reveal();
        if (announce) {
            if (winnerHand.equals(playerHand)) context.send(context.getTranslated("blackjack.end.won"));
            else context.send(context.getTranslated("blackjack.end.lost"));
        }
    }

    private void dealerWins() {
        dealerWins(true);
    }

    private void dealerWins(boolean announce) {
        gameOver=true;
        winnerHand = dealerHand;
        endReveal(announce);
        playerMoney -= playerBet;
    }

    private void playerWins() {
        gameOver=true;
        winnerHand = playerHand;
        endReveal();
        playerMoney -= playerBet;
    }
}
