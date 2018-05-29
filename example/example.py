import os
import contextlib

import time
import requests
from subprocess import Popen
from logging import getLogger


API_URL = "http://localhost:6001"
TOOL_DIR = os.path.abspath(os.path.dirname(__file__))
TOOL_DIR = os.path.abspath(
    os.path.join(
        os.path.dirname(__file__),
        "../build/install"))
TOOL_CMD = "pdfbox-server/bin/pdfbox-server"


@contextlib.contextmanager
def start():
    logger = getLogger(__name__)

    try:
        logger.info([TOOL_CMD, TOOL_DIR])

        with open(os.devnull, 'w') as devnull:
            proc = Popen(TOOL_CMD, shell=True, cwd=TOOL_DIR, stderr=devnull)
            logger.info("pdfbox server pid = %s" % proc.pid)
            assert proc.poll() is None
            time.sleep(5)   # とりあえず最初に多少待つ

            for i in range(10, 0, -1):
                try:
                    time.sleep(1)
                    requests.head("%s/ping" % API_URL)
                    break
                except requests.exceptions.ConnectionError as _:
                    logger.warn("... waiting server %d" % i)
            else:
                raise RuntimeError("pdfbox server start failed")

            logger.info("pdfbox server has been ready.")
            yield API_URL

            assert proc.poll() is None
    except Exception as e:
        logger.exception("pdfbox server error has occurred")
        raise
    finally:
        proc.terminate()
        proc.wait()
        assert proc.poll() is not None
        logger.info("pdfbox server terminated.")


def pdf_to_text(buf, api_url=API_URL):
    logger = getLogger(__name__)

    try:
        resp = requests.post(api_url, data=buf)
        if resp.status_code != 200:
            raise RuntimeError("pdfbox-server convert failed")

        assert resp.encoding == 'utf-8'
        return resp.text
    except requests.exceptions.ConnectionError as e:
        logger.exception("pdfbox-server connection failed")


if __name__ == '__main__':
    from logging import StreamHandler, INFO
    logger = getLogger(__name__)
    handler = StreamHandler()
    handler.setLevel(INFO)
    logger.setLevel(INFO)
    logger.addHandler(handler)

    with start() as api_url:
        with open("sample.pdf", "rb") as fp:
            text = pdf_to_text(fp.read(), api_url)
            print(text)
