import { expect } from 'chai';
import { readFileSync } from 'node:fs';

// Guards the #529 migration: the Save-form and configuration toolbars must render their button icons
// from the self-contained --sbb-*-icon tokens (CSS-background <span>s), never from Polarion's runtime
// raster assets (/polarion/ria/images/actions/*.gif). mocha runs with cwd = app.
const JSP_DIR = 'src/main/resources/META-INF/resources/common/jsp';
const BUTTONS = readFileSync(`${JSP_DIR}/buttons.jsp`, 'utf8');
const CONFIGURATIONS = readFileSync(`${JSP_DIR}/configurations.jsp`, 'utf8');

describe('action-button icons (buttons.jsp / configurations.jsp)', function () {

    it('reference no raster GIF/PNG/JPG icons and no /polarion/ria runtime assets', function () {
        for (const [name, jsp] of [['buttons.jsp', BUTTONS], ['configurations.jsp', CONFIGURATIONS]]) {
            expect(jsp, `${name} still references a raster icon`).to.not.match(/\.(gif|png|jpg)\b/i);
            expect(jsp, `${name} still points at Polarion runtime assets`).to.not.contain('/polarion/ria/images/');
        }
    });

    it('buttons.jsp uses the self-made SVG icon classes for Save / Cancel / Default / Revisions', function () {
        for (const cls of ['sbb-icon-save', 'sbb-icon-cancel', 'sbb-icon-revert', 'sbb-icon-select-revision']) {
            expect(BUTTONS, `buttons.jsp missing ${cls}`).to.contain(cls);
        }
    });

    it('configurations.jsp uses the self-made SVG icon classes for Rename / Delete / Cancel / Save', function () {
        for (const cls of ['sbb-icon-edit', 'sbb-icon-delete', 'sbb-icon-cancel', 'sbb-icon-save']) {
            expect(CONFIGURATIONS, `configurations.jsp missing ${cls}`).to.contain(cls);
        }
    });
});
