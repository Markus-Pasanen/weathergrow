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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import java.util.ArrayList;
import java.util.List;

public class ShopWindow {
    private static final float W = 720f;
    private static final float H = 1280f;
    private static final float CARD_W = 186f;
    private static final float BTN_W = 168f;
    private static final float BTN_H = 48f;

    private final Skin skin;
    private final Stage stage;
    private final Window window;
    private Table content;
    private Label coinLabel;
    private final List<ShopItem> items;
    private final ShopCallback callback;
    private final TextureRegionDrawable itemIcon;

    private final Texture shopBgTex;
    private final Texture cardBgTex;
    private final Texture cardBorderTex;
    private final Texture toastBgTex;
    private final Texture badgeTex;
    private final Texture coinTex;
    private final Texture btnGreenTex;
    private final Texture btnGreenDownTex;
    private final Texture btnDisabledTex;

    private Table toastContainer;
    private boolean toastShowing;

    private final List<TextButton> buyButtons = new ArrayList<>();
    private final List<Table> cardRefs = new ArrayList<>();

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

        shopBgTex = new Texture(Gdx.files.internal("backgrounds/shop.png"));
        cardBgTex = new Texture(Gdx.files.internal("backgrounds/card.png"));
        cardBorderTex = createRoundRectTex(
            (int)cardBgTex.getWidth() + 4,
            (int)cardBgTex.getHeight() + 4,
            14, new Color(0.22f, 0.24f, 0.32f, 1f));
        toastBgTex = createRoundRectTex(400, 60, 10, new Color(0.04f, 0.04f, 0.06f, 0.94f));
        badgeTex = createRoundRectTex(120, 36, 5, new Color(1f, 0.55f, 0f, 1f));
        coinTex = createCoinTex(32);
        btnGreenTex = createRoundRectTex((int)BTN_W, (int)BTN_H, 10, new Color(0.12f, 0.72f, 0.28f, 1f));
        btnGreenDownTex = createRoundRectTex((int)BTN_W, (int)BTN_H, 10, new Color(0.07f, 0.52f, 0.18f, 1f));
        btnDisabledTex = createRoundRectTex((int)BTN_W, (int)BTN_H, 10, new Color(0.25f, 0.25f, 0.28f, 0.65f));

        window = new Window("", skin);
        window.setBackground((Drawable) null);
        window.setModal(true);
        window.setVisible(false);
        window.setMovable(false);
        window.setSize(W, H);
        window.setPosition(0, 0);
        window.pad(0);

        itemIcon = loadItemIcon();
        buildContent();
    }

    private TextureRegionDrawable loadItemIcon() {
        try {
            Texture tex = new Texture(Gdx.files.internal("ui/Icons/shop2.png"));
            TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
            d.setMinWidth(72);
            d.setMinHeight(72);
            return d;
        } catch (Exception e) {
            Pixmap p = new Pixmap(72, 72, Pixmap.Format.RGBA8888);
            p.setColor(0.35f, 0.6f, 0.85f, 1f);
            p.fillCircle(36, 36, 34);
            Texture tex = new Texture(p);
            p.dispose();
            return new TextureRegionDrawable(new TextureRegion(tex));
        }
    }

    private void buildContent() {
        content = new Table();
        content.setFillParent(true);
        content.setBackground(new TextureRegionDrawable(new TextureRegion(shopBgTex)));

        Table header = new Table();

        coinLabel = new Label("0", skin, "white");
        coinLabel.setColor(1f, 0.84f, 0.2f, 1f);
        coinLabel.setFontScale(1.15f);

        Image headerCoin = new Image(new TextureRegionDrawable(new TextureRegion(coinTex)));
        headerCoin.setScaling(Scaling.fit);

        Table coinRow = new Table();
        coinRow.add(headerCoin).size(22);
        coinRow.add(coinLabel).padLeft(5);

        ImageButton closeBtn = makeCloseBtn();

        header.add(coinRow).padLeft(20);
        header.add().expandX();
        header.add(closeBtn).size(30).padRight(20);
        content.add(header).expandX().fillX().padTop(140).padBottom(20).row();

        int n = items.size();
        float cardW = (W - n * 8) / n;

        Table itemsRow = new Table();
        itemsRow.defaults().padLeft(4).padRight(4);
        for (int i = 0; i < n; i++) {
            boolean badge = (i == 1);
            itemsRow.add(buildCard(items.get(i), badge, cardW));
        }
        content.add(itemsRow).expand().center().row();

        window.add(content).expand().fill();
    }

    private ImageButton makeCloseBtn() {
        Pixmap p = new Pixmap(30, 30, Pixmap.Format.RGBA8888);
        p.setColor(1f, 1f, 1f, 0.12f);
        p.fillCircle(15, 15, 14);
        p.setColor(1f, 1f, 1f, 0.8f);
        int s = 9, e = 21;
        for (int t = -1; t <= 1; t++) {
            p.drawLine(s + t, s, e + t, e);
            p.drawLine(s + t, e, e + t, s);
        }
        Texture tex = new Texture(p);
        p.dispose();

        ImageButton.ImageButtonStyle s2 = new ImageButton.ImageButtonStyle();
        s2.up = new TextureRegionDrawable(new TextureRegion(tex));
        ImageButton btn = new ImageButton(s2);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                hide();
            }
        });
        return btn;
    }

    private Table buildCard(ShopItem item, boolean showBadge, float cardW) {
        float stackW = cardW - 4;
        float stackH = stackW * cardBgTex.getHeight() / cardBgTex.getWidth();

        TextureRegionDrawable borderDrw = new TextureRegionDrawable(new TextureRegion(cardBorderTex));
        borderDrw.setMinWidth(0);
        borderDrw.setMinHeight(0);

        Table card = new Table();
        card.setBackground(borderDrw);
        card.pad(2);

        Table innerContent = new Table();
        innerContent.pad(10, 6, 10, 6);

        if (showBadge) {
            Stack iconStack = new Stack();
            Image icon = new Image(itemIcon);
            icon.setScaling(Scaling.fit);
            iconStack.add(icon);

            Table badgeTable = new Table();
            badgeTable.setBackground(new TextureRegionDrawable(new TextureRegion(badgeTex)));
            Label badgeLabel = new Label("BEST", skin);
            badgeLabel.setColor(1f, 1f, 1f, 1f);
            badgeLabel.setFontScale(0.5f);
            badgeTable.add(badgeLabel).pad(2, 5, 2, 5);

            iconStack.add(badgeTable);
            innerContent.add(iconStack).size(56).center().padBottom(5).row();
        } else {
            Image icon = new Image(itemIcon);
            icon.setScaling(Scaling.fit);
            innerContent.add(icon).size(56).center().padBottom(5).row();
        }

        Label nameLbl = new Label(item.name, skin);
        nameLbl.setColor(1f, 1f, 1f, 1f);
        nameLbl.setFontScale(0.8f);
        nameLbl.setAlignment(Align.center);
        innerContent.add(nameLbl).expandX().center().padBottom(3).row();

        Label descLbl = new Label(item.description, skin);
        descLbl.setColor(0.5f, 0.52f, 0.6f, 1f);
        descLbl.setFontScale(0.55f);
        descLbl.setWrap(true);
        descLbl.setAlignment(Align.center);
        float descW = stackW - 12;
        innerContent.add(descLbl).width(descW).center().padBottom(6).row();

        Table priceRow = new Table();
        Image pCoin = new Image(new TextureRegionDrawable(new TextureRegion(coinTex)));
        pCoin.setScaling(Scaling.fit);
        Label priceLbl = new Label(String.valueOf(item.price), skin);
        priceLbl.setColor(1f, 0.84f, 0.2f, 1f);
        priceLbl.setFontScale(0.9f);
        priceRow.add(pCoin).size(14);
        priceRow.add(priceLbl).padLeft(3);

        innerContent.add(priceRow).center().padBottom(6).row();

        TextButton buyBtn = makeBuyBtn(item);
        buyButtons.add(buyBtn);
        innerContent.add(buyBtn).size(BTN_W, BTN_H).center().row();

        Stack stack = new Stack();

        Image cardBgImage = new Image(cardBgTex);
        cardBgImage.setScaling(Scaling.fit);

        Table inner = new Table();
        inner.add(innerContent).expand().center();

        stack.add(cardBgImage);
        stack.add(inner);
        card.add(stack).size(stackW, stackH);
        cardRefs.add(card);
        return card;
    }

    private TextButton makeBuyBtn(ShopItem item) {
        TextButton.TextButtonStyle s2 = new TextButton.TextButtonStyle();
        s2.up = new TextureRegionDrawable(new TextureRegion(btnGreenTex));
        s2.down = new TextureRegionDrawable(new TextureRegion(btnGreenDownTex));
        s2.font = skin.getFont("default");
        s2.fontColor = Color.WHITE;
        s2.disabled = new TextureRegionDrawable(new TextureRegion(btnDisabledTex));
        s2.disabledFontColor = new Color(0.5f, 0.5f, 0.5f, 1f);

        TextButton btn = new TextButton("BUY", s2);
        btn.getLabel().setFontScale(0.85f);
        btn.pad(6);

        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (btn.isDisabled()) return;
                btn.addAction(Actions.sequence(
                    Actions.scaleTo(0.93f, 0.93f, 0.04f),
                    Actions.scaleTo(1f, 1f, 0.06f),
                    Actions.run(() -> onBuy(item))
                ));
            }
        });

        return btn;
    }

    private void onBuy(ShopItem item) {
        if (callback == null) return;
        int coins = callback.getPlayerCoins();
        if (coins < item.price) {
            showToast("Need more coins!");
            shakeCard(item);
            return;
        }
        if (callback.onPurchase(item, item.price)) {
            showToast("Item purchased!");
        } else {
            showToast("Purchase failed!");
        }
        refresh();
    }

    private void shakeCard(ShopItem item) {
        int idx = items.indexOf(item);
        if (idx < 0 || idx >= cardRefs.size()) return;
        Table card = cardRefs.get(idx);
        card.addAction(Actions.sequence(
            Actions.moveBy(-5, 0, 0.025f),
            Actions.moveBy(10, 0, 0.025f),
            Actions.moveBy(-10, 0, 0.025f),
            Actions.moveBy(10, 0, 0.025f),
            Actions.moveBy(-5, 0, 0.025f)
        ));
    }

    private void showToast(String msg) {
        if (toastShowing && toastContainer != null) {
            toastContainer.remove();
        }
        Label toastLabel = new Label(msg, skin);
        toastLabel.setColor(1f, 1f, 1f, 1f);
        toastLabel.setFontScale(1.0f);

        toastContainer = new Table();
        toastContainer.setBackground(new TextureRegionDrawable(new TextureRegion(toastBgTex)));
        toastContainer.add(toastLabel).pad(10, 22, 10, 22);
        toastContainer.pack();

        toastContainer.setPosition(
            (W - toastContainer.getPrefWidth()) / 2,
            H * 0.24f
        );

        window.addActor(toastContainer);
        toastShowing = true;
        toastContainer.addAction(Actions.sequence(
            Actions.alpha(0f),
            Actions.fadeIn(0.15f),
            Actions.delay(1.4f),
            Actions.fadeOut(0.25f),
            Actions.run(() -> {
                toastShowing = false;
                toastContainer.remove();
            })
        ));
    }

    private void refresh() {
        updateCoinDisplay();
        updateBuyButtons();
    }

    private void updateBuyButtons() {
        int coins = callback != null ? callback.getPlayerCoins() : 0;
        for (int i = 0; i < buyButtons.size() && i < items.size(); i++) {
            buyButtons.get(i).setDisabled(coins < items.get(i).price);
        }
    }

    // --- Texture factories ---

    private static Texture createSolidTexture(Color c) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture createGradientTexture(int w, int h, Color top, Color bot) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            float t = (float) y / (h - 1);
            float r = top.r + (bot.r - top.r) * t;
            float g = top.g + (bot.g - top.g) * t;
            float b = top.b + (bot.b - top.b) * t;
            float a = top.a + (bot.a - top.a) * t;
            p.setColor(r, g, b, a);
            p.drawLine(0, y, w - 1, y);
        }
        Texture tex = new Texture(p);
        p.dispose();
        return tex;
    }

    private static Texture createRoundRectTex(int w, int h, int r, Color c) {
        int tw = Math.max(w, r * 2 + 1);
        int th = Math.max(h, r * 2 + 1);
        Pixmap p = new Pixmap(tw, th, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fillRectangle(r, 0, tw - r * 2, th);
        p.fillRectangle(0, r, tw, th - r * 2);
        p.fillCircle(r, r, r);
        p.fillCircle(tw - r - 1, r, r);
        p.fillCircle(r, th - r - 1, r);
        p.fillCircle(tw - r - 1, th - r - 1, r);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture createCoinTex(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        int cx = size / 2, cy = size / 2, cr = size / 2 - 1;
        p.setColor(1f, 0.9f, 0.2f, 1f);
        p.fillCircle(cx, cy, cr);
        p.setColor(0.7f, 0.6f, 0.08f, 1f);
        p.drawCircle(cx, cy, cr);
        p.setColor(1f, 0.95f, 0.5f, 0.5f);
        p.fillCircle(cx - cr / 3, cy + cr / 3, cr / 3);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    // --- Show / Hide ---

    public void show() {
        refresh();
        window.setVisible(true);
        window.getColor().a = 0f;
        window.addAction(Actions.fadeIn(0.2f));
        if (content != null) {
            content.setScale(0.88f);
            content.addAction(Actions.scaleTo(1f, 1f, 0.22f));
        }
    }

    public void hide() {
        window.addAction(Actions.sequence(
            Actions.fadeOut(0.12f),
            Actions.run(() -> window.setVisible(false))
        ));
    }

    public void updateCoinDisplay() {
        if (callback != null) {
            coinLabel.setText(String.valueOf(callback.getPlayerCoins()));
        }
    }

    public boolean isVisible() { return window.isVisible(); }
    public Window getWindow() { return window; }

    public void dispose() {
        shopBgTex.dispose();
        cardBgTex.dispose();
        cardBorderTex.dispose();
        toastBgTex.dispose();
        badgeTex.dispose();
        coinTex.dispose();
        btnGreenTex.dispose();
        btnGreenDownTex.dispose();
        btnDisabledTex.dispose();
    }
}
