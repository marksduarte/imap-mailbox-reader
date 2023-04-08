package dev.marksduarte.mailboxreader;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.mail.*;
import java.util.stream.Stream;

public class ImapMailboxReader {

    /* filename */
    private static final String FILENAME = System.getProperty("user.dir") + "/tmp/collectedAddresses.csv";

    private static final File FILE = new File(FILENAME);

    /* login data */
    private static final String STORE = "imap.gmail.com";
    private static final String FOLDER = "[Gmail]/Todos os e-mails";

    /* select addresses to extract */
    private static final Boolean GETFROM = true;
    private static final Boolean GETTO = false;
    private static final Boolean GETCC = false;
    private static final Boolean GETBCC = false;

    /* select addresses to filter */
    private static final Boolean FILTER_FROM = true;
    private static final Boolean FILTER_TO = false;

    /* filter addresses to extract */
    private static final String[] ADDRESS_FILTER = {
            "noreply@", "no-reply@", "no.reply@", "donotreply@", "do_not_reply@", "webmaster@",
            "account-noreply@"
            /* filters here */
    };

    public static void main(String[] args) {
        final String username = args[0];
        final String password = args[1];
        if (username == null || password == null) throw new RuntimeException("Username and password are required.");
        getAddresses(username, password);
    }

    private static void getAddresses(final String user, final String password) {
        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");

        System.out.println(">> Connecting to email server");

        try {
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect(STORE, user, password);

            // Choose folder to work in
            Folder myfolder = store.getFolder(FOLDER);
            myfolder.open(Folder.READ_ONLY);

            Message[] messages = myfolder.getMessages();

            System.out.printf(">> Quantity of e-mails in folder %s: %d\n", FOLDER, messages.length);

            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];

                System.out.printf("\r%s%d\t%.2f%s", ">> Progress: ", i, calculatePercentage(i, messages.length),"%");

                if (GETFROM) {
                    Address[] fromAddresses = message.getFrom();
                    if (fromAddresses != null)
                        appendAddressesToFile(fromAddresses, FILTER_FROM);
                }

                if (GETTO) {
                    Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
                    if (toAddresses != null)
                        appendAddressesToFile(toAddresses, FILTER_TO);
                }

                if (GETCC) {
                    Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
                    if (ccAddresses != null)
                        appendAddressesToFile(ccAddresses);
                }

                if (GETBCC) {
                    Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
                    if (bccAddresses != null)
                        appendAddressesToFile(bccAddresses);
                }
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.exit(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(">> Finished!");
    }

    private static double calculatePercentage(double obtained, double total) {
        return obtained * 100 / total;
    }

    private static boolean filter(Address address) {
        String addressString = address.toString();
        for (String filter : ADDRESS_FILTER) {
            if (addressString.contains(filter)) {
                return false;
            }
        }
        return true;
    }

    private static String cleanAddress(String address) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            if (address.contains("<")) {
                stringBuilder.append(address.split("<")[0].trim());
                stringBuilder.append(";");
                stringBuilder.append(address.substring(address.lastIndexOf("<") + 1, address.indexOf(">")).trim());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return stringBuilder.toString().toLowerCase();
    }

    private static void appendAddressesToFile(Address[] addresses) throws IOException {
        appendAddressesToFile(addresses, false);
    }

    private static void appendAddressesToFile(Address[] addresses, boolean filter) throws IOException {
        final Set<String> addressSet = new HashSet<>();
        if (filter) {
            Stream.of(addresses)
                    .filter(ImapMailboxReader::filter)
                    .forEach(address -> {
                        addressSet.add(cleanAddress(address.toString()));
                    });
        } else {
            Stream.of(addresses).forEach(address -> {
                addressSet.add(cleanAddress(address.toString()));
            });
        }
        if (addressSet.size() > 0)
            FileUtils.writeLines(FILE, StandardCharsets.UTF_8.toString(), addressSet, true);
    }
}
