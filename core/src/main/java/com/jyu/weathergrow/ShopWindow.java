package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import java.util.ArrayList;
import java.util.List;

public class ShopWindow {
    private static final float W = 720f;
    private static final float H = 1280f;

    private final Skin skin;
    private final Stage stage;
    private final Window window;
    private Label coinLabel;
    private final List<ShopItem> items;
    private final ShopCallback callback;
    private final TextureRegionDrawable itemIcon;
    private final Texture cardTexture;
    private final Texture bgTexture;
    private final Texture toastBgTexture;

    private Label toastLabel;
    private boolean toastShowing;
    private Table toastContainer;

    public interface ShopCallback {
        boolean onPurchase(ShopItem item, int price);
        int getPlayerCoins();
    }

    public static class ShopItem {
        public final String id;
        public final String name;
        public final int price;
        public final String description;

        public ShopItem(String id, String name, int price, String description) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.description = description;
        }
    }

    public ShopWindow(Skin skin, Stage stage, List<ShopItem> items, ShopCallback callback) {
        this.skin = skin;
        this.stage = stage;
        this.items = new ArrayList<>(items);
        this.callback = callback;

        cardTexture = createRoundRectTexture(1, 1, 10, new Color(0.18f, 0.18f, 0.2f, 1f));
        toastBgTexture = createRoundRectTexture(1, 1, 8, new Color(0.1f, 0.1f, 0.12f, 0.92f));

        bgTexture = new Texture(Gdx.files.internal("backgrounds/shop.png"));
        TextureRegionDrawable bg = new TextureRegionDrawable(new TextureRegion(bgTexture));

        window = new Window("", skin);
        window.setBackground(bg);
        window.setModal(true);
        window.setVisible(false);
        window.setMovable(false);
        window.setSize(W, H);
        window.pad(0);

        itemIcon = loadItemIcon();
        buildContent();
        centerWindow();
    }

    private TextureRegionDrawable loadItemIcon() {
        try {
            Texture tex = new Texture(Gdx.files.internal("ui/Icons/shop2.png"));
            TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
            d.setMinWidth(64);
            d.setMinHeight(64);
            return d;
        } catch (Exception e) {
            Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            p.setColor(0.35f, 0.6f, 0.85f, 1f);
            p.fillCircle(32, 32, 30);
            Texture tex = new Texture(p);
            p.dispose();
            return new TextureRegionDrawable(new TextureRegion(tex));
        }
    }

    private void buildContent() {
        Table root = new Table();
        root.pad(24);

        // Header
        Label title = new Label("SHOP", skin);
        title.getStyle().font.getData().setScale(1.8f);
        title.setColor(1f, 1f, 1f, 1f);

        ImageButton closeBtn = createCloseButton();
        Table header = new Table();
        header.add(title).expandX().left();
        header.add(closeBtn).size(36).right();
        root.add(header).expandX().fillX().padBottom(12).row();

        // Coin bar
        coinLabel = new Label("0", skin);
        coinLabel.getStyle().font.getData().setScale(1.3f);
        coinLabel.setColor(1f, 0.85f, 0.2f, 1f);

        Image coinIcon = new Image(createCoinTexture());
        coinIcon.setScaling(Scaling.fit);

        Table coinRow = new Table();
        coinRow.add(coinIcon).size(28);
        coinRow.add(coinLabel).padLeft(6);
        root.add(coinRow).expandX().left().padLeft(8).padBottom(20).row();

        // Items
        Table itemTable = new Table();
        for (ShopItem item : items) {
            itemTable.add(buildItemCard(item)).expandX().fillX().padBottom(12).row();
        }

        ScrollPane scroll = new ScrollPane(itemTable, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill().row();

        window.add(root).expand().fill();
    }

    private ImageButton createCloseButton() {
        Pixmap p = new Pixmap(36, 36, Pixmap.Format.RGBA8888);
        p.setColor(0.7f, 0.2f, 0.2f, 1f);
        p.fillCircle(18, 18, 17);
        p.setColor(0.5f, 0.1f, 0.1f, 1f);
        p.drawCircle(18, 18, 17);
        Texture tex = new Texture(p);
        p.dispose();

        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));

        // Draw X on the texture
        Pixmap xp = new Pixmap(36, 36, Pixmap.Format.RGBA8888);
        xp.setColor(0.7f, 0.2f, 0.2f, 1f);
        xp.fillCircle(18, 18, 17);
        xp.setColor(0.5f, 0.1f, 0.1f, 1f);
        xp.drawCircle(18, 18, 17);
        xp.setColor(1f, 1f, 1f, 1f);
        xp.drawLine(11, 11, 25, 25);
        xp.drawLine(12, 11, 26, 25);
        xp.drawLine(11, 12, 25, 26);
        xp.drawLine(25, 11, 11, 25);
        xp.drawLine(24, 11, 10, 25);
        xp.drawLine(25, 12, 11, 26);
        Texture xtex = new Texture(xp);
        xp.dispose();

        TextureRegionDrawable xd = new TextureRegionDrawable(new TextureRegion(xtex));

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = xd;
        style.down = xd;

        ImageButton btn = new ImageButton(style);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                hide();
            }
        });
        return btn;
    }

    private Table buildItemCard(ShopItem item) {
        Table card = new Table();
        card.setBackground(new TextureRegionDrawable(new TextureRegion(cardTexture)));
        card.pad(10);

        Image icon = new Image(itemIcon);
        icon.setScaling(Scaling.fit);

        Label nameLbl = new Label(item.name, skin);
        nameLbl.getStyle().font.getData().setScale(1.1f);
        nameLbl.setColor(1f, 1f, 1f, 1f);

        Label descLbl = new Label(item.description, skin);
        descLbl.getStyle().font.getData().setScale(0.8f);
        descLbl.setColor(0.6f, 0.6f, 0.65f, 1f);
        descLbl.setWrap(true);

        Table info = new Table();
        info.add(nameLbl).expandX().left().padBottom(2).row();
        info.add(descLbl).expandX().fillX().left();

        Label priceLbl = new Label(item.price + " coins", skin);
        priceLbl.getStyle().font.getData().setScale(0.9f);
        priceLbl.setColor(1f, 0.85f, 0.2f, 1f);

        TextButton buyBtn = new TextButton("BUY", skin);
        buyBtn.getLabel().getStyle().font.getData().setScale(0.95f);
        buyBtn.getLabel().setColor(1f, 1f, 1f, 1f);
        buyBtn.pad(6, 16, 6, 16);

        int fp = item.price;
        buyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                handlePurchase(item, fp);
            }
        });

        Table actions = new Table();
        actions.add(priceLbl).padBottom(4).row();
        actions.add(buyBtn).size(90, 36);

        // Use fixed widths so text wrapping works: icon(64) + gap(12) + info(~280) + gap + actions(90)
        card.add(icon).size(64).padRight(12);
        card.add(info).width(280).padRight(12);
        card.add(actions).right();

        return card;
    }

    private void handlePurchase(ShopItem item, int price) {
        if (callback == null) return;
        if (callback.getPlayerCoins() < price) {
            showToast("Not enough coins!");
            return;
        }
        if (callback.onPurchase(item, price)) {
            showToast("Purchased!");
        } else {
            showToast("Purchase failed!");
        }
        updateCoinDisplay();
    }

    private void showToast(String msg) {
        if (toastShowing && toastContainer != null) {
            toastContainer.remove();
        }
        toastLabel = new Label(msg, skin);
        toastLabel.getStyle().font.getData().setScale(1.1f);
        toastLabel.setColor(1f, 1f, 1f, 1f);

        toastContainer = new Table();
        toastContainer.setBackground(new TextureRegionDrawable(new TextureRegion(toastBgTexture)));
        toastContainer.add(toastLabel).pad(10, 20, 10, 20);
        toastContainer.pack();

        toastContainer.setPosition(
            (window.getWidth() - toastContainer.getPrefWidth()) / 2,
            window.getHeight() * 0.35f
        );

        window.addActor(toastContainer);
        toastShowing = true;
        toastContainer.addAction(Actions.sequence(
            Actions.delay(1.0f),
            Actions.fadeOut(0.3f),
            Actions.run(() -> {
                toastShowing = false;
                toastContainer.remove();
            })
        ));
    }

    private Texture createCoinTexture() {
        Pixmap p = new Pixmap(28, 28, Pixmap.Format.RGBA8888);
        p.setColor(1f, 0.85f, 0.2f, 1f);
        p.fillCircle(14, 14, 12);
        p.setColor(0.8f, 0.65f, 0.1f, 1f);
        p.drawCircle(14, 14, 13);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private Texture createRoundRectTexture(int w, int h, int r, Color c) {
        Pixmap p = new Pixmap(Math.max(w, 1), Math.max(h, 1), Pixmap.Format.RGBA8888);
        p.setColor(c);
        if (w > r * 2 && h > r * 2) {
            p.fillRectangle(r, 0, w - r * 2, h);
            p.fillRectangle(0, r, w, h - r * 2);
            p.fillCircle(r, r, r);
            p.fillCircle(w - r - 1, r, r);
            p.fillCircle(r, h - r - 1, r);
            p.fillCircle(w - r - 1, h - r - 1, r);
        } else {
            p.fill();
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public void show() {
        updateCoinDisplay();
        window.setVisible(true);
        window.setScale(0.9f);
        window.getColor().a = 0f;
        window.addAction(Actions.parallel(
            Actions.fadeIn(0.15f),
            Actions.scaleTo(1f, 1f, 0.15f)
        ));
    }

    public void hide() {
        window.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeOut(0.1f),
                Actions.scaleTo(0.9f, 0.9f, 0.1f)
            ),
            Actions.run(() -> window.setVisible(false))
        ));
    }

    public void updateCoinDisplay() {
        if (callback != null) {
            coinLabel.setText(String.valueOf(callback.getPlayerCoins()));
        }
    }

    private void centerWindow() {
        window.setPosition(0, 0);
    }

    public boolean isVisible() { return window.isVisible(); }
    public Window getWindow() { return window; }

    public void dispose() {
        bgTexture.dispose();
        cardTexture.dispose();
        toastBgTexture.dispose();
    }
}
