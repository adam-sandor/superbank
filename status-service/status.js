import express from 'express';
import fetch from 'node-fetch';

const app = express();

app.get('/statuses', async (req, res) => {
    try {
        const urls = ['http://account/status', 'http://accountholder/status', 'http://permissions/status'];
        const results = await Promise.all(urls.map(url => fetch(url)));
        const statusCodes = await Promise.all(results.map(result => result.status));
        res.status(200).json({ "Account Service": statusCodes[0], "AccountHolder Service:": statusCodes[1],
            "Permissions Service": statusCodes[2] });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/status', async (req, res) => {
    res.status(200).json({ status: 'OK' });
});

app.listen(8080, () => console.log('Server listening on port 8080'));